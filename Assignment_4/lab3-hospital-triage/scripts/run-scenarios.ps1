<#
.SYNOPSIS
  Starts Lab 3 hospital-triage instances from the JSON files in ..\test-data.

.DESCRIPTION
  preferredDate in every request is replaced with tomorrow's date, so the requests stay valid
  whenever the script is run.

  "all" runs every scenario except 05 (scheduling API failure), because that one switches the
  mock scheduling API off and would also break the other instances. Run it on its own:

    .\run-scenarios.ps1 -Scenario 05

  Then correct the cause with .\fix-services.ps1 and click Retry on the incident in Operate.
#>
param(
  [string]$Scenario = "all",
  [string]$BaseUrl = "http://localhost:8083"
)

$tomorrow = (Get-Date).AddDays(1).ToString("yyyy-MM-dd")

$files = Get-ChildItem (Join-Path $PSScriptRoot "..\test-data\*.json") | Sort-Object Name |
  Where-Object {
    if ($Scenario -eq "all") { -not $_.BaseName.StartsWith("05") } else { $_.BaseName -like "$Scenario*" }
  }

if (-not $files) { throw "No test-data file matches '$Scenario'" }

foreach ($file in $files) {
  if ($file.BaseName.StartsWith("05")) {
    Invoke-RestMethod -Method Put -Uri "$BaseUrl/api/mock/scheduling/available/false" | Out-Null
    "SIMULATED FAILURE: mock scheduling API is now DOWN (HTTP 503)"
  }
  $request = Get-Content $file.FullName -Raw | ConvertFrom-Json
  $request.preferredDate = $tomorrow
  $body = $request | ConvertTo-Json
  $response = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/appointment-requests" -ContentType "application/json" -Body $body
  "{0,-30} -> process instance {1} (request {2})" -f $file.BaseName, $response.processInstanceKey, $response.requestId
}

if ($Scenario -ne "all" -and $Scenario.StartsWith("05")) {
  ""
  "Wait for the incident on 'Retrieve available slots' in Operate (~20-30 s), inspect it, then run"
  "  .\fix-services.ps1"
  "and click 'Retry' on the incident in Operate."
}

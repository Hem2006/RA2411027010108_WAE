<#
.SYNOPSIS
  Starts Lab 2 order-processing instances from the JSON files in ..\test-data.

.DESCRIPTION
  "all" runs every scenario except the two failure scenarios (06, 07), because those switch a
  mock service off and would also break the other instances. Run them one at a time:

    .\run-scenarios.ps1 -Scenario 06   # payment gateway switched off -> connector incident
    .\run-scenarios.ps1 -Scenario 07   # inventory service switched off -> worker incident

  Then correct the cause with .\fix-services.ps1 and click Retry on the incident in Operate.
#>
param(
  [string]$Scenario = "all",
  [string]$BaseUrl = "http://localhost:8082"
)

$failureScenarios = @{
  "06" = @{ Url = "/api/mock/payments/available/false"; Text = "mock payment gateway is now DOWN (HTTP 503)" }
  "07" = @{ Url = "/api/mock/inventory/available/false"; Text = "mock inventory service is now DOWN (HTTP 503)" }
}

$files = Get-ChildItem (Join-Path $PSScriptRoot "..\test-data\*.json") | Sort-Object Name |
  Where-Object {
    $prefix = $_.BaseName.Substring(0, 2)
    if ($Scenario -eq "all") { -not $failureScenarios.ContainsKey($prefix) } else { $_.BaseName -like "$Scenario*" }
  }

if (-not $files) { throw "No test-data file matches '$Scenario'" }

foreach ($file in $files) {
  $prefix = $file.BaseName.Substring(0, 2)
  if ($failureScenarios.ContainsKey($prefix)) {
    Invoke-RestMethod -Method Put -Uri ($BaseUrl + $failureScenarios[$prefix].Url) | Out-Null
    "SIMULATED FAILURE: " + $failureScenarios[$prefix].Text
  }
  $body = Get-Content $file.FullName -Raw
  $response = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/orders" -ContentType "application/json" -Body $body
  "{0,-34} -> process instance {1} (order {2})" -f $file.BaseName, $response.processInstanceKey, $response.orderId
}

if ($Scenario -ne "all" -and $failureScenarios.ContainsKey($Scenario.Substring(0, 2))) {
  ""
  "Wait for the incident in Operate (retries are used up after ~20-30 s), inspect it, then run"
  "  .\fix-services.ps1"
  "and click 'Retry' on the incident in Operate."
}

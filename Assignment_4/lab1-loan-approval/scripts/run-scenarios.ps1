<#
.SYNOPSIS
  Starts Lab 1 loan-approval process instances from the JSON files in ..\test-data.

.EXAMPLE
  .\run-scenarios.ps1                 # all scenarios
  .\run-scenarios.ps1 -Scenario 06    # only 06-worker-failure
#>
param(
  [string]$Scenario = "all",
  [string]$BaseUrl = "http://localhost:8081"
)

$files = Get-ChildItem (Join-Path $PSScriptRoot "..\test-data\*.json") |
  Where-Object { $Scenario -eq "all" -or $_.BaseName -like "$Scenario*" } |
  Sort-Object Name

if (-not $files) { throw "No test-data file matches '$Scenario'" }

foreach ($file in $files) {
  $body = Get-Content $file.FullName -Raw
  $response = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/loan-applications" -ContentType "application/json" -Body $body
  "{0,-28} -> process instance {1} (application {2})" -f $file.BaseName, $response.processInstanceKey, $response.applicationId
}

if ($files.BaseName -match "^06") {
  ""
  "06-worker-failure: monthlyIncome is 'six thousand', so 'Validate applicant' fails twice and raises an incident."
  "Fix in Operate: open the instance > Variables > edit monthlyIncome to 6000 > Save > Retry the incident."
}

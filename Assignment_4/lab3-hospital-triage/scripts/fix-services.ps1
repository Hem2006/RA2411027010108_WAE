<#
.SYNOPSIS
  Corrective action for the Lab 3 failure scenario: brings the mock scheduling API back up.
  Afterwards retry the incident in Operate.
#>
param([string]$BaseUrl = "http://localhost:8083")

Invoke-RestMethod -Method Put -Uri "$BaseUrl/api/mock/scheduling/available/true" | Out-Null
"Mock scheduling API is UP again. Now click 'Retry' on the incident in Operate."

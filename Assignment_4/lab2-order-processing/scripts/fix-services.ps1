<#
.SYNOPSIS
  Corrective action for the Lab 2 failure scenarios: brings the mock payment gateway and the
  mock inventory service back up. Afterwards retry the incident in Operate.
#>
param([string]$BaseUrl = "http://localhost:8082")

Invoke-RestMethod -Method Put -Uri "$BaseUrl/api/mock/payments/available/true" | Out-Null
Invoke-RestMethod -Method Put -Uri "$BaseUrl/api/mock/inventory/available/true" | Out-Null
"Mock payment gateway and inventory service are UP again. Now click 'Retry' on the incident in Operate."

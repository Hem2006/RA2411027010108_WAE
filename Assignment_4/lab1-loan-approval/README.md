# Lab 1 – Loan Approval

Loan applications go through a BPMN process with a DMN decision, Spring Boot job workers, a REST Connector call to a mock credit API, a manual review task in Tasklist, and incident recovery. More detail is in [REPORT.md](REPORT.md).

## Files

| Path | What it is |
|---|---|
| `models/loan-approval.bpmn` | the process, `loan-approval-process` |
| `models/loan-decision.dmn` | decision `loan-eligibility-decision` (hit policy FIRST, rules R1 to R6) |
| `models/loan-review.form` | Tasklist form `loan-review-form` for manual review |
| `worker/` | Spring Boot app: the workers `lab1-validate-applicant` and `lab1-notify-applicant`, the mock credit API, and `POST /api/loan-applications` to start a process |
| `test-data/*.json` | one fake application per scenario |
| `scripts/run-scenarios.ps1` | starts the scenarios |
| `api-examples.http` | example REST requests |

## Running it

Set up the cluster first, see the [root README](../README.md).

```bash
cd worker
mvn test                                             # unit tests
mvn spring-boot:run                                  # local Camunda 8 Run
mvn spring-boot:run -Dspring-boot.run.profiles=saas  # SaaS (needs the CAMUNDA_* env vars, a tunnel and the LAB1_MOCK_API_URL secret)
```

When the app starts it deploys everything in `models/` and logs the process, decision and form versions.

Then start scenarios from the `scripts` folder in PowerShell:

```powershell
.\run-scenarios.ps1               # all 7
.\run-scenarios.ps1 -Scenario 03  # just one
```

## The scenarios

| # | File | What happens |
|---|---|---|
| 01 | `01-approved` | Finishes by itself at Loan approved (DMN rule R5). |
| 02 | `02-rejected-low-credit` | Finishes by itself at Loan rejected (rule R2, credit score 515). |
| 03 | `03-manual-review` | Stops at Review loan application. In Tasklist, claim it, pick Approve or Reject, add a comment and complete it. |
| 04 | `04-credit-api-error` | The mock API returns HTTP 503. The connector's error expression throws the BPMN error `CREDIT_API_ERROR`, the boundary event catches it, and the application goes to manual review with `reviewReason = CREDIT_SERVICE_UNAVAILABLE`. |
| 05 | `05-credit-api-timeout` | The mock API waits 15 s, which is longer than the connector's 10 s read timeout. Same error path as 04. |
| 06 | `06-worker-failure` | `monthlyIncome` is `"six thousand"`, so the validation worker fails the job twice and an incident is raised. To fix it in Operate: open the instance, go to Variables, change `monthlyIncome` to `6000`, save, then click Retry on the incident. It carries on and gets approved. |
| 07 | `07-invalid-underage` | Age 17 fails the business validation (no incident) and ends at Loan rejected. |

You can also force the mock credit API to fail or work with `PUT /api/mock/credit/mode/ERROR` (or `TIMEOUT`, or `NORMAL`).

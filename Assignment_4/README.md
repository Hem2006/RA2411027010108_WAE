# PBJ-2 Practical Assignment – Camunda 8 Workflows

Three small workflow projects for Camunda 8.8. Each one has a BPMN process, a DMN decision table, some forms, a Spring Boot worker for the code steps, and a mock REST API that the REST Connector calls. Test data is included for every scenario.

| Lab | Folder | Process ID | Port |
|-----|--------|------------|------|
| 1 – Loan approval | [lab1-loan-approval](lab1-loan-approval/README.md) | `loan-approval-process` | 8081 |
| 2 – Order processing | [lab2-order-processing](lab2-order-processing/README.md) | `order-processing-process` | 8082 |
| 3 – Hospital triage | [lab3-hospital-triage](lab3-hospital-triage/README.md) | `hospital-triage-process` | 8083 |

Each lab folder looks like this:

```
models/             BPMN, DMN and form files (open in Camunda Modeler)
worker/             Spring Boot app (job workers, mock API, endpoint to start a process)
test-data/          fake JSON input, one file per scenario
scripts/            PowerShell scripts to start scenarios and fix the simulated outages
api-examples.http   example REST calls
screenshots/        my screenshots go here (checklist inside)
REPORT.md           the write-up
```

All the data is made up. No real payment info, credentials or patient data anywhere.

## What you need

- Java 21+ and Maven 3.9+
- Camunda Modeler 5.x if you want to open the models
- A Camunda 8.8 cluster. I used Camunda 8 Run locally, but SaaS works too.

## Running locally (Camunda 8 Run)

This is the way I ran everything.

1. Unzip Camunda 8 Run 8.8.
2. Set the connector secrets as environment variables first, then start it. The connector runtime reads them from the environment, so setting them after won't work.
   ```powershell
   $env:LAB1_MOCK_API_URL="http://localhost:8081"
   $env:LAB2_MOCK_API_URL="http://localhost:8082"
   $env:LAB3_MOCK_API_URL="http://localhost:8083"
   .\c8run.exe start
   ```
   It takes a few minutes to start up.
3. In another terminal, start one lab's app:
   ```bash
   cd lab1-loan-approval/worker
   mvn spring-boot:run
   ```
   Only run one lab at a time. Ctrl+C the app before starting a different lab.
4. Operate is at http://localhost:8080/operate and Tasklist is at http://localhost:8080/tasklist. Login is `demo` / `demo`.

## Running on Camunda 8 SaaS

1. Create a 8.8 cluster in Camunda Console.
2. Make an API client (cluster > API tab > Create new client) with the orchestration cluster scope. Depending on the Console version it may show up as Zeebe and/or Orchestration Cluster API. Then set the values in PowerShell:
   ```powershell
   $env:CAMUNDA_CLIENT_ID="..."; $env:CAMUNDA_CLIENT_SECRET="..."
   $env:CAMUNDA_CLUSTER_ID="..."; $env:CAMUNDA_CLUSTER_REGION="..."   # e.g. bru-2
   ```
3. The REST Connector runs in Camunda's cloud on SaaS, so it can't reach localhost. You need a tunnel, for example [cloudflared](https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/do-more-with-tunnels/trycloudflare/) (no account needed):
   ```bash
   cloudflared tunnel --url http://localhost:8081
   ```
   It gives you a public `https://<random>.trycloudflare.com` URL. ngrok does the same thing. The workers don't need a tunnel since they connect out to the cluster.
4. Add connector secrets in Console (cluster > Connector Secrets):

   | Secret name | Value |
   |---|---|
   | `LAB1_MOCK_API_URL` | tunnel URL for port 8081, no trailing `/` |
   | `LAB2_MOCK_API_URL` | tunnel URL for port 8082 |
   | `LAB3_MOCK_API_URL` | tunnel URL for port 8083 |

5. Start a lab with the saas profile. It deploys that lab's models on startup:
   ```bash
   cd lab1-loan-approval/worker
   mvn spring-boot:run -Dspring-boot.run.profiles=saas
   ```
   You can also deploy from Desktop Modeler with the same client. Don't forget the DMN and form files, not just the BPMN.
6. Run `scripts/run-scenarios.ps1`, then check Operate and Tasklist in Console.

## Testing

I ran all 22 scenarios (7 + 8 + 7) end to end on Camunda 8 Run locally, including the four incidents that get fixed and retried. The results are in the test table in each lab's `REPORT.md`. I haven't run it on SaaS. Only the connection settings and secret values should differ.

Screenshots still have to come from my own cluster. Each lab's `screenshots/README.md` lists which ones.

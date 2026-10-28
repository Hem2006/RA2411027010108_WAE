# Lab 3 – Hospital Appointment and Triage

This is a classroom simulation with fake patients only (`SYN-PAT-nnnn`). It is not medical advice.

Flow: register the patient, validate, triage with the DMN (Routine, Priority Review or Urgent Assessment), then staff review or an immediate assessment, then a call to the mock scheduling API through the REST Connector, and finally either booking the slot or sending it to manual scheduling. More detail is in [REPORT.md](REPORT.md).

## Files

| Path | What it is |
|---|---|
| `models/hospital-triage.bpmn` | the process, `hospital-triage-process` |
| `models/triage-category.dmn` | decision `triage-category-decision` (hit policy FIRST, rules T1 to T11) |
| `models/priority-review.form`, `urgent-assessment.form`, `manual-scheduling.form` | Tasklist forms for the three user tasks |
| `worker/` | Spring Boot app: job workers, the mock scheduling API, and `POST /api/appointment-requests` to start a process |
| `test-data/*.json` | one fake request per scenario |
| `scripts/run-scenarios.ps1`, `scripts/fix-services.ps1` | start the scenarios, and bring the scheduling API back after the simulated outage |
| `api-examples.http` | example REST requests |

## Running it

Set up the cluster first, see the [root README](../README.md).

```bash
cd worker
mvn test
mvn spring-boot:run                                  # local Camunda 8 Run
mvn spring-boot:run -Dspring-boot.run.profiles=saas  # SaaS (CAMUNDA_* env vars, a tunnel to 8083 and the LAB3_MOCK_API_URL secret)
```

Then, from the `scripts` folder:

```powershell
.\run-scenarios.ps1                # 01 to 04, 06, 07
.\run-scenarios.ps1 -Scenario 05   # scheduling API outage, gives an incident
.\fix-services.ps1                 # fixes the outage, then click Retry in Operate
```

The script swaps `preferredDate` for tomorrow's date, so the requests are always valid.

## The scenarios

| # | File | What happens |
|---|---|---|
| 01 | `01-routine-slot-available` | No urgency flags, so ROUTINE. Slots are found and one is booked. Ends at Appointment booked. |
| 02 | `02-priority-review` | Pain level 6 gives PRIORITY_REVIEW. A Priority staff review task appears in Tasklist, you pick Approve, and a slot within 3 days is booked. |
| 03 | `03-urgent-assessment` | Chest pain gives URGENT_ASSESSMENT, with an Immediate clinical assessment task (priority 100). |
| 04 | `04-no-slots-available` | Dermatology is fully booked, so `slotCount=0` and a Manual scheduling review task is created. |
| 05 | `05-scheduling-api-failure` | The scheduling API returns 503 and the connector runs out of retries, giving an incident on Retrieve available slots. Run `fix-services.ps1`, click Retry in Operate, and it books. |
| 06 | `06-priority-escalated` | Age 80 with a fever gives PRIORITY_REVIEW. Staff choose Escalate, which goes to an immediate assessment. |
| 07 | `07-invalid-registration` | `patientRef` isn't a synthetic one, so validation fails and the request is closed without triage. |

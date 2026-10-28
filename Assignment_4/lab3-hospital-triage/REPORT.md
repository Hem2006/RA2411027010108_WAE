# Lab 3 Report – Hospital Appointment and Patient Triage Management

> Educational simulation. All patients are synthetic (`SYN-PAT-nnnn`, no names or real identifiers). The triage rules are predefined teaching rules, **not clinical guidance**.

## 1. Architecture

```
  POST /api/appointment-requests ──► Spring Boot app (port 8083)
                                      ├─ AppointmentRequestController → creates process instance
                                      ├─ TriageWorkers  lab3-validate-registration, lab3-book-appointment, lab3-notify-patient
                                      └─ SchedulingController (mock hospital scheduling API)
                                              ▲ GET /api/scheduling/slots   (REST Connector, Camunda connectors runtime)
                                              ▲ POST /api/scheduling/bookings (booking worker, via SchedulingClient)

  Camunda 8.8: Zeebe (BPMN + DMN triage-category-decision) · Connectors · Tasklist (3 staff tasks) · Operate
```

![BPMN](screenshots/01-bpmn-model.png)

## 2. Execution flow and routing

| Step | Element | Notes |
|---|---|---|
| 1 | **Appointment request received** | patient reference, preference, symptoms summary, urgency indicators |
| 2 | **Validate registration** – worker | synthetic `patientRef` format, age 0–120, department, time of day, ISO `preferredDate` (today … +90 days), symptoms ≤ 500 chars, pain 0–10. Normalises missing flags to `false` and numbers to 0 |
| 3 | **Registration valid?** | `=registrationValid = true` → triage · default → *invalid* → notify |
| 4 | **Triage request (DMN)** | `triageCategory`, `triageReason`, `maxWaitDays` |
| 5 | **Triage category?** | `URGENT_ASSESSMENT` → **Immediate clinical assessment** · `PRIORITY_REVIEW` → **Priority staff review** · default `ROUTINE` → scheduling |
| 6 | **Priority staff review** (group `triage-nurses`, priority 75) | `staffDecision` = APPROVE_SCHEDULING or ESCALATE_URGENT |
| 7 | **Staff decision?** | `=staffDecision = "ESCALATE_URGENT"` → immediate assessment · default → scheduling |
| 8 | **Immediate clinical assessment** (group `triage-clinicians`, priority 100) | `assessmentOutcome`, `clinicianNotes` → notify |
| 9 | **Retrieve available slots (REST)** | GET slots within `maxWaitDays` → `availableSlots`, `slotCount`. 5xx → retries → **incident** |
| 10 | **Slot available?** | `=slotCount > 0` → **Book appointment** · default → **Manual scheduling review** |
| 11 | **Book appointment** – worker | books the first slot → `bookingId`, `bookingStatus`, `bookedSlot` |
| 12 | **Manual scheduling review** (group `scheduling-staff`) | `schedulingOutcome` = WAITLISTED / ALTERNATIVE_ARRANGED / REFERRED_EXTERNALLY |
| 13 | **Notify patient** – worker | derives `requestStatus`, simulated SMS |
| 14 | **Request status?** | `APPOINTMENT_BOOKED` → **Appointment booked** · default → **Request closed by staff / rejected** |

## 3. DMN – `triage-category-decision` (hit policy FIRST, educational rules)

| # | chestPain | breathingDifficulty | severeBleeding | painScore | highFever | patientAge | symptomDurationDays | → category | maxWaitDays |
|---|---|---|---|---|---|---|---|---|---|
| T1 | true | – | – | – | – | – | – | URGENT_ASSESSMENT | 0 |
| T2 | – | true | – | – | – | – | – | URGENT_ASSESSMENT | 0 |
| T3 | – | – | true | – | – | – | – | URGENT_ASSESSMENT | 0 |
| T4 | – | – | – | ≥ 8 | – | – | – | URGENT_ASSESSMENT | 0 |
| T5 | – | – | – | – | true | < 2 | – | URGENT_ASSESSMENT | 0 |
| T6 | – | – | – | – | true | ≥ 65 | – | PRIORITY_REVIEW | 2 |
| T7 | – | – | – | [5..7] | – | – | – | PRIORITY_REVIEW | 3 |
| T8 | – | – | – | – | true | – | – | PRIORITY_REVIEW | 3 |
| T9 | – | – | – | – | – | ≥ 75 | – | PRIORITY_REVIEW | 5 |
| T10 | – | – | – | – | – | – | > 14 | PRIORITY_REVIEW | 7 |
| T11 | – | – | – | – | – | – | – | ROUTINE | 14 |

Red-flag rules come first, so any red flag wins over the softer indicators. `maxWaitDays` becomes the slot-search window (`withinDays`).

**Sample evaluations (verification run):**

| Case | Key inputs | Rule | Output |
|---|---|---|---|
| TC01 | no flags, pain 2, age 34 | T11 | ROUTINE, 14 days |
| TC02 | pain 6, age 52 | T7 | PRIORITY_REVIEW, 3 days |
| TC03 | chestPain = true, pain 7 | T1 | URGENT_ASSESSMENT, 0 |
| TC06 | highFever = true, age 80 | T6 | PRIORITY_REVIEW, 2 days |

## 4. Mock scheduling API and REST Connector

**Connector configuration (`Retrieve available slots`)**

| Property | Value |
|---|---|
| Template | REST Outbound Connector `io.camunda.connectors.HttpJson.v2` v11 |
| Method / URL | `GET {{secrets.LAB3_MOCK_API_URL}}/api/scheduling/slots` |
| Headers | `={"Accept": "application/json", "X-Request-Id": requestId}` |
| Query parameters | `={department: department, fromDate: preferredDate, timeOfDay: preferredTimeOfDay, withinDays: string(maxWaitDays)}` |
| Timeouts | 10 s / 10 s |
| Result expression | `={availableSlots: response.body.slots, slotCount: count(response.body.slots)}` |
| Retries / backoff | 3 / PT10S → incident when the API stays down |

**Mock API behaviour (synthetic):**
- Lead time per department: GENERAL_MEDICINE and PAEDIATRICS 1 day, CARDIOLOGY 3, ORTHOPAEDICS 5. DERMATOLOGY is fully booked.
- Weekday slots at 09:00 and 10:30 (MORNING) and 14:00 and 15:30 (AFTERNOON). At most 5 slots, only within `withinDays` of `fromDate`. Booked slots are removed.
- `PUT /api/mock/scheduling/available/{true|false}` switches the simulated outage.

Request (connector):
```http
GET /api/scheduling/slots?department=GENERAL_MEDICINE&fromDate=2026-11-02&timeOfDay=MORNING&withinDays=3
Accept: application/json
X-Request-Id: AR-DEMO
```
Response 200:
```json
{"department":"GENERAL_MEDICINE","fromDate":"2026-11-02","withinDays":3,"slots":[
 {"slotId":"GEN-20261103-0900","start":"2026-11-03T09:00","clinician":"Dr Synthetic A"},
 {"slotId":"GEN-20261103-1030","start":"2026-11-03T10:30","clinician":"Dr Synthetic B"},
 {"slotId":"GEN-20261104-0900","start":"2026-11-04T09:00","clinician":"Dr Synthetic B"},
 {"slotId":"GEN-20261104-1030","start":"2026-11-04T10:30","clinician":"Dr Synthetic C"},
 {"slotId":"GEN-20261105-0900","start":"2026-11-05T09:00","clinician":"Dr Synthetic C"}]}
```
Booking (worker): `POST /api/scheduling/bookings {"slotId":"GEN-20261105-0900","patientRef":"SYN-PAT-0099","requestId":"AR-DEMO"}` →
`{"bookingId":"BK-8C4D4ADD","slotId":"GEN-20261105-0900","patientRef":"SYN-PAT-0099","status":"CONFIRMED"}` (a second booking of the same slot → 409).

Outage → `503 {"status":503,"error":"Service Unavailable","path":"/api/scheduling/slots"}`.

## 5. Process-variable dictionary

| Variable | Type | Set by | Purpose |
|---|---|---|---|
| requestId | String | start | Business key, request header |
| patientRef | String | start | Synthetic reference `SYN-PAT-nnnn` |
| patientAge | Number | start | DMN input |
| department | String | start | GENERAL_MEDICINE, CARDIOLOGY, DERMATOLOGY, ORTHOPAEDICS, PAEDIATRICS |
| preferredDate | String (ISO date) | start | Slot search start |
| preferredTimeOfDay | String | start / validate | MORNING, AFTERNOON, ANY |
| symptomsSummary | String | start | Free-text synthetic summary (≤ 500 chars) |
| chestPain, breathingDifficulty, severeBleeding, highFever | Boolean | start / validate | Urgency indicators (DMN inputs) |
| painScore | Integer | start / validate | 0–10 (DMN input) |
| symptomDurationDays | Integer | start / validate | DMN input |
| registrationValid | Boolean | validate worker | Gateway *Registration valid?* |
| registrationErrors | List&lt;String&gt; | validate worker | Validation messages |
| triageResult | Context | DMN task | Full DMN output |
| triageCategory | String | DMN task | ROUTINE / PRIORITY_REVIEW / URGENT_ASSESSMENT (gateway) |
| triageReason | String | DMN task | Matched rule |
| maxWaitDays | Number | DMN task | Slot search window |
| staffDecision, staffNotes | String | Priority review form | APPROVE_SCHEDULING / ESCALATE_URGENT (gateway) |
| assessmentOutcome, clinicianNotes | String | Urgent assessment form | Outcome of the immediate assessment |
| availableSlots | List&lt;Context&gt; | REST connector | Slots returned by the scheduling API |
| slotCount | Number | REST connector | Gateway *Slot available?* |
| bookingId, bookingStatus, bookedSlot | String, String, Context | booking worker | Booking result |
| schedulingOutcome, schedulingNotes | String | Manual scheduling form | Outcome when no slot was available |
| requestStatus | String | notify worker | APPOINTMENT_BOOKED, URGENT_ASSESSED_*, MANUAL_*, REGISTRATION_REJECTED |
| patientNotified, notificationChannel, notifiedAt | Boolean, String, String | notify worker | Simulated notification audit |

## 6. Test cases and results

Executed on Camunda 8.8.44 (Camunda 8 Run, local) on 2026-10-28 (preferredDate = 2026-10-29).

| TC | Input | Expected | Actual | Status |
|---|---|---|---|---|
| 01 | Routine, GENERAL_MEDICINE, morning | ROUTINE → slot found → booked | T11 ROUTINE, 5 slots, booked GEN-20261030-0900, `APPOINTMENT_BOOKED` (2251799813687422) | PASS |
| 02 | Pain 6, age 52 | PRIORITY_REVIEW → staff review task → approve → booked | T7, task completed with APPROVE_SCHEDULING → booked GEN-20261030-1030 (2251799813687472) | PASS |
| 03 | Chest pain | URGENT_ASSESSMENT → immediate assessment task | T1, *Immediate clinical assessment* task (priority 100) → outcome DIRECTED_TO_EMERGENCY (2251799813687512) | PASS |
| 04 | DERMATOLOGY (fully booked) | No slots → manual scheduling task | `slotCount=0` → *Manual scheduling review* → WAITLISTED, `MANUAL_WAITLISTED` (2251799813687531) | PASS |
| 05 | PAEDIATRICS + scheduling API down | Connector fails → incident → fix → retry → booked | Incident `JOB_NO_RETRIES` on *Retrieve available slots* (HTTP 503). API restored, incident retried → 5 slots → booked (2251799813687986) | PASS |
| 06 | Age 80 + high fever | PRIORITY_REVIEW → escalate → immediate assessment | T6 → staff chose ESCALATE_URGENT → immediate assessment → SAME_DAY_SLOT_ARRANGED (2251799813687608) | PASS |
| 07 | `patientRef = "PATIENT-123"` | Validation fails → closed, no triage | `registrationValid=false`, `REGISTRATION_REJECTED` (2251799813687637) | PASS |

Unit tests: `mvn test` – 4 tests (validation incl. non-synthetic ID and past date, slot generation incl. lead time and fully booked department), all passing.

## 7. Incident investigation and recovery (TC05)

1. Operate shows the instance *Active* with an incident on **Retrieve available slots (REST)**, type *Job: No retries left*.
2. The incident message contains the HTTP 503 response body from `/api/scheduling/slots`, which shows the scheduling service is down. The request variables (`department`, `preferredDate`, `maxWaitDays`) are correct.
3. Corrective action: restore the scheduling service (`scripts/fix-services.ps1` → `PUT /api/mock/scheduling/available/true`).
4. **Retry** in Operate → the connector fetches the slots again → *Book appointment* → *Appointment booked*. The incident is shown as resolved in the instance history.

## 8. Assumptions

- `ROUTINE` requests are scheduled automatically within 14 days. `PRIORITY_REVIEW` always needs a staff decision before booking.
- `URGENT_ASSESSMENT` never goes to automatic booking. It always goes to an immediate human assessment in the simulated workflow.
- The first returned slot is booked automatically. The patient's slot choice is not modelled.

## 9. Limitations

- The scheduling API is a mock with generated, in-memory slots. Bookings are lost when the app restarts.
- The triage rules are simplified teaching rules and must not be used for real patients.
- No timer escalation if a staff task is not completed in time. A boundary timer on the user tasks would be the next improvement.
- Candidate groups are set on the tasks. Restricting who can see them depends on the cluster's Identity and authorization setup.
- On SaaS, the REST Connector needs a public tunnel to reach the local mock API.

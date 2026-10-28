# Lab 3 – screenshots to capture

Take these from **your own** running cluster. Save them in this folder with these names.

| File name | Where | What must be visible |
|---|---|---|
| `01-bpmn-model.png` | Camunda Modeler | Whole `hospital-triage.bpmn` diagram |
| `02-dmn-triage-table.png` | Modeler | `triage-category.dmn`, rules T1–T11 |
| `03-rest-connector-config.png` | Modeler, *Retrieve available slots (REST)* selected | GET URL with `{{secrets.LAB3_MOCK_API_URL}}`, query parameters, result expression |
| `04-deployment.png` | Modeler deploy dialog **or** app console log | Process, decision and 3 forms deployed |
| `05-operate-processes.png` | Operate → Processes | All 7 instances |
| `06-operate-routine-booked.png` | Operate → AR-TC01 | Path to *Appointment booked*, variables `triageCategory`, `availableSlots`, `bookingStatus` |
| `07-tasklist-priority-review.png` | Tasklist | *Priority staff review* task (AR-TC02) with the form |
| `08-tasklist-urgent-assessment.png` | Tasklist | *Immediate clinical assessment* task (AR-TC03), priority shown |
| `09-tasklist-manual-scheduling.png` | Tasklist | *Manual scheduling review* task (AR-TC04), `slotCount = 0` |
| `10-operate-escalation.png` | Operate → AR-TC06 | Path Priority review → escalate → Immediate assessment |
| `11-operate-scheduling-incident.png` | Operate → AR-TC05 | Incident on *Retrieve available slots* (HTTP 503) |
| `12-operate-scheduling-recovered.png` | Operate → AR-TC05 | After fix + Retry: completed at *Appointment booked* |
| `13-operate-activity-history.png` | Operate → any instance | Instance history panel (activity history) |

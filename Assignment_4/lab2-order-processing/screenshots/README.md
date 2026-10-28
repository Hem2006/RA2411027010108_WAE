# Lab 2 – screenshots to capture

Take these from **your own** running cluster. Save them in this folder with these names.

| File name | Where | What must be visible |
|---|---|---|
| `01-bpmn-model.png` | Camunda Modeler | Whole `order-processing.bpmn` diagram |
| `02-dmn-priority-table.png` | Modeler | `order-priority.dmn`, rules P1–P8 |
| `03-rest-connector-config.png` | Modeler, *Process payment (REST)* selected | URL with `{{secrets.LAB2_MOCK_API_URL}}`, method, headers, body, result expression, retries/backoff |
| `04-deployment.png` | Modeler deploy dialog **or** app console log | Process, decision and form deployed |
| `05-operate-processes.png` | Operate → Processes | All 8 instances |
| `06-operate-fulfilled.png` | Operate → ORD-TC01 | Path to *Order sent to fulfilment*, variables `orderTotal`, `deliveryPriority`, `paymentStatus`, `shipmentId` |
| `07-operate-out-of-stock.png` | Operate → ORD-TC02 | *insufficient stock* path, `stockAvailable=false` |
| `08-operate-payment-declined.png` | Operate → ORD-TC03 | DECLINED path, `paymentDeclineReason` |
| `09-tasklist-verification.png` | Tasklist | *Verify high-value order* task (ORD-TC04) with the form |
| `10-operate-payment-incident.png` | Operate → ORD-TC06 | Incident on *Process payment* (HTTP 503) |
| `11-operate-payment-recovered.png` | Operate → ORD-TC06 | After fix + Retry: completed, incident resolved |
| `12-operate-inventory-incident.png` | Operate → ORD-TC07 | Incident on *Check inventory* (`503 Service Unavailable`) |
| `13-operate-inventory-recovered.png` | Operate → ORD-TC07 | After fix + Retry: completed |

# Lab 1 – screenshots to capture

Take these from **your own** running cluster. Save them in this folder with these names, so the report can refer to them.

| File name | Where | What must be visible |
|---|---|---|
| `01-bpmn-model.png` | Camunda Modeler | Whole `loan-approval.bpmn` diagram |
| `02-rest-connector-config.png` | Modeler, *Assess credit (REST)* selected | Properties panel: URL with `{{secrets.LAB1_MOCK_API_URL}}`, method, body, result and error expressions |
| `03-dmn-table.png` | Modeler | `loan-decision.dmn` decision table, rules R1–R6 |
| `04-deployment.png` | Modeler deploy dialog **or** app console log | Successful deployment of process, decision and form |
| `05-operate-processes.png` | Operate → Processes | All 7 instances listed with their states |
| `06-operate-approved.png` | Operate → instance LA-TC01 | Completed path to *Loan approved* plus the Variables panel (`creditScore`, `loanDecision`, `finalStatus`) |
| `07-operate-rejected.png` | Operate → instance LA-TC02 | Path to *Loan rejected* |
| `08-operate-credit-error.png` | Operate → instance LA-TC04 | Path through the *Credit API error / timeout* boundary event, `reviewReason` variable |
| `09-tasklist-review-task.png` | Tasklist | *Review loan application* task with the form filled in |
| `10-operate-incident.png` | Operate → instance LA-TC06 | Incident on *Validate applicant* with the error message `monthlyIncome must be numeric…` |
| `11-operate-fix-variable.png` | Operate → instance LA-TC06 → Variables | `monthlyIncome` being edited to `6000` |
| `12-operate-recovered.png` | Operate → instance LA-TC06 | After **Retry**: instance completed at *Loan approved*, incident resolved |
| `13-operate-decision.png` | Operate → Decisions | `loan-eligibility-decision` evaluation showing inputs and the matched rule |

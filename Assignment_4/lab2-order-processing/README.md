# Lab 2 – Order Processing

Flow: validate the order, check the (simulated) inventory, work out the total, get a priority from the DMN, send high-value or odd orders to manual verification, take a mock payment through the REST Connector, then either fulfil or cancel. More detail is in [REPORT.md](REPORT.md).

## Files

| Path | What it is |
|---|---|
| `models/order-processing.bpmn` | the process, `order-processing-process` |
| `models/order-priority.dmn` | decision `order-priority-decision` (hit policy FIRST, rules P1 to P8) |
| `models/order-verification.form` | Tasklist form `order-verification-form` |
| `worker/` | Spring Boot app: 5 job workers, a mock inventory service, a mock payment gateway, and `POST /api/orders` to start a process |
| `test-data/*.json` | one fake order per scenario |
| `scripts/run-scenarios.ps1`, `scripts/fix-services.ps1` | start the scenarios, and bring the mock services back after a simulated outage |
| `api-examples.http` | example REST requests |

## Running it

Set up the cluster first, see the [root README](../README.md).

```bash
cd worker
mvn test
mvn spring-boot:run                                  # local Camunda 8 Run
mvn spring-boot:run -Dspring-boot.run.profiles=saas  # SaaS (CAMUNDA_* env vars, a tunnel to 8082 and the LAB2_MOCK_API_URL secret)
```

Then, from the `scripts` folder:

```powershell
.\run-scenarios.ps1                # 01 to 05 and 08 (the normal and business-error paths)
.\run-scenarios.ps1 -Scenario 06   # payment gateway outage, gives an incident
.\run-scenarios.ps1 -Scenario 07   # inventory service outage, gives an incident
.\fix-services.ps1                 # fixes the outage, then click Retry in Operate
```

Run 06 and 07 one at a time. They switch a mock service off, so any other order running at the same moment would fail too.

## The scenarios

| # | File | What happens |
|---|---|---|
| 01 | `01-stock-ok-payment-ok` | In stock, total 209.37, priority MEDIUM (P6), payment APPROVED, then fulfilment. Ends at Order sent to fulfilment. |
| 02 | `02-insufficient-stock` | Asks for 5 x P-500 but only 3 are in stock. Ends at Order not fulfilled (`CANCELLED_OUT_OF_STOCK`). |
| 03 | `03-payment-declined` | Token `tok_test_declined` makes the mock gateway answer `DECLINED`. Ends at Order not fulfilled (`PAYMENT_DECLINED`). |
| 04 | `04-high-value-manual-review` | Total 6 182.75 is over 5 000 (P1), so a Verify high-value order task shows up in Tasklist. Approve it and it carries on to payment and fulfilment. |
| 05 | `05-new-customer-review-rejected` | NEW customer with a total of 2 487.24, over 2 000 (P2). Verification task, you reject it, ends as `CANCELLED_BY_VERIFICATION`. |
| 06 | `06-payment-service-failure` | Gateway returns HTTP 503 and the connector runs out of retries, so there's an incident on Process payment. Run `fix-services.ps1`, click Retry in Operate, and the payment goes through and the order is fulfilled (CRITICAL priority). |
| 07 | `07-inventory-service-failure` | Inventory returns HTTP 503 and the worker job fails, so there's an incident on Check inventory. Run `fix-services.ps1`, click Retry, and it's fulfilled. |
| 08 | `08-invalid-payment-token` | The token isn't a `tok_test_*` mock token, so validation fails and the order is `REJECTED_INVALID`. |

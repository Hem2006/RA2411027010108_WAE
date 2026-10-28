# Lab 2 Report – End-to-End E-Commerce Order Processing and Fulfilment

## 1. Architecture

```
  POST /api/orders ──► Spring Boot app (port 8082)
                        ├─ OrderController            → creates process instance
                        ├─ OrderWorkers               lab2-validate-order, lab2-check-inventory, lab2-calculate-total
                        ├─ FulfilmentWorkers          lab2-arrange-fulfilment, lab2-notify-customer
                        ├─ InventoryController (mock)  ◄── HTTP from InventoryClient (inside the workers)
                        └─ PaymentController  (mock)  ◄── HTTP from the REST Connector (Camunda connectors runtime)

  Camunda 8.8: Zeebe (BPMN + DMN order-priority-decision) · Connectors (io.camunda:http-json:1) · Tasklist · Operate
```

- One external Spring Boot application with five `@JobWorker`s (Java 21, `camunda-spring-boot-starter` 8.8).
- The **inventory service** is called by the job worker over HTTP. When it is unavailable this is a worker failure, and the job retries lead to an incident.
- The **payment gateway** is called by the **REST Connector**, with the URL from connector secret `LAB2_MOCK_API_URL`. No real card data is ever used: the validator and the mock gateway accept only `tok_test_*` tokens.

![BPMN](screenshots/01-bpmn-model.png)

## 2. Execution flow and gateway routing

| Step | Element | Notes |
|---|---|---|
| 1 | **Order received** (start) | order variables |
| 2 | **Validate order** – worker `lab2-validate-order` | product exists, quantity 1–20, delivery preference, customer category, `tok_test_*` token. Sets `orderValid`, `productName`, `unitPrice`, `currency` |
| 3 | **Order valid?** | `=orderValid = true` → continue · default → *invalid* → notify |
| 4 | **Check inventory** – worker `lab2-check-inventory` | `GET /api/inventory/{productId}?quantity=` → `stockAvailable`, `stockOnHand`, `warehouse`. HTTP error → job fails (retries 2) → **incident** |
| 5 | **In stock?** | `=stockAvailable = true` → continue · default → *insufficient stock* → notify (order cannot reach fulfilment) |
| 6 | **Calculate order total** – worker `lab2-calculate-total` | subtotal + shipping (STANDARD 4.99, free from 100; EXPRESS 14.99; SAME_DAY 29.99) + 8 % tax → `orderTotal` |
| 7 | **Determine order priority (DMN)** | `deliveryPriority`, `manualVerificationRequired`, `priorityReason` |
| 8 | **Manual verification required?** | `=manualVerificationRequired = true` → user task · default → payment |
| 9 | **Verify high-value order** (user task, group `order-verification-team`) | form sets `verificationDecision`, `verificationNotes` |
| 10 | **Verified?** | `=verificationDecision = "APPROVED"` → payment · default → *rejected* → notify |
| 11 | **Process payment (REST)** | REST Connector → `paymentId`, `paymentStatus`, `paymentDeclineReason`. HTTP 5xx → 3 retries (10 s backoff) → **incident** |
| 12 | **Payment approved?** | `=paymentStatus = "APPROVED"` → fulfilment · default → *DECLINED* → notify |
| 13 | **Arrange fulfilment** – worker `lab2-arrange-fulfilment` | reserves stock, creates `shipmentId`, `plannedDispatchDate` (CRITICAL 0 days, HIGH 1, MEDIUM 2, LOW 3) |
| 14 | **Notify customer** – worker `lab2-notify-customer` | derives `orderStatus` from the path taken |
| 15 | **Order status?** | `=orderStatus = "FULFILMENT_SCHEDULED"` → **Order sent to fulfilment** · default → **Order not fulfilled** |

All gateways are exclusive gateways and each has a default flow, so every instance always has a path.

## 3. DMN – `order-priority-decision` (hit policy FIRST)

| # | orderTotal | customerCategory | deliveryPreference | deliveryPriority | manualVerificationRequired | Reason |
|---|---|---|---|---|---|---|
| P1 | > 5000 | – | – | HIGH | **true** | high-value order |
| P2 | > 2000 | "NEW" | – | MEDIUM | **true** | new customer, large first order |
| P3 | – | "PLATINUM" | "SAME_DAY" | CRITICAL | false | platinum + same-day |
| P4 | – | – | "SAME_DAY" | HIGH | false | same-day requested |
| P5 | – | "GOLD","PLATINUM" | "EXPRESS" | HIGH | false | loyal customer + express |
| P6 | – | – | "EXPRESS" | MEDIUM | false | express requested |
| P7 | > 1000 | "GOLD","PLATINUM" | – | MEDIUM | false | loyal customer, order > 1000 |
| P8 | – | – | – | LOW | false | standard order |

The verification rules come first so that a high-value or suspicious order is always checked, whatever its delivery option.

**Sample evaluations (verification run):**

| Case | orderTotal, category, delivery | Rule | Output |
|---|---|---|---|
| TC01 | 209.37, REGULAR, EXPRESS | P6 | MEDIUM, no verification |
| TC03 | 31.99, REGULAR, STANDARD | P8 | LOW, no verification |
| TC04 | 6182.75, GOLD, SAME_DAY | P1 | HIGH, **verification** |
| TC05 | 2487.24, NEW, STANDARD | P2 | MEDIUM, **verification** |
| TC06 | 385.31, PLATINUM, SAME_DAY | P3 | CRITICAL, no verification |

## 4. REST Connector configuration and API sample

| Property | Value |
|---|---|
| Template | REST Outbound Connector `io.camunda.connectors.HttpJson.v2` v11 (`io.camunda:http-json:1`) |
| Authentication | None (mock service) |
| Method / URL | `POST {{secrets.LAB2_MOCK_API_URL}}/api/payments` |
| Headers | `={"Content-Type": "application/json", "Idempotency-Key": orderId}` |
| Body | `={orderId, customerRef, amount: orderTotal, currency, paymentToken}` |
| Timeouts | 10 s / 10 s |
| Result expression | `={paymentId: response.body.paymentId, paymentStatus: response.body.status, paymentDeclineReason: response.body.declineReason}` |
| Retries / backoff | 3 / PT10S. No error expression, so 5xx errors are retried and then become an incident |

The `Idempotency-Key` header makes the connector's retries safe. The mock returns the stored result for a repeated key instead of charging twice.

Request:
```http
POST /api/payments
Content-Type: application/json
Idempotency-Key: ORD-TC01

{"orderId":"ORD-TC01","customerRef":"CUST-1001","amount":209.37,"currency":"USD","paymentToken":"tok_test_visa_ok"}
```
Response 200 (approved):
```json
{"paymentId":"PAY-B0538C3A","orderId":"ORD-TC01","amount":209.37,"currency":"USD","status":"APPROVED","declineReason":null,"processedAt":"2026-10-28T08:24:26+05:30"}
```
Response 200 (declined, token `tok_test_declined` or amount > 10 000):
```json
{"paymentId":"PAY-496A6B7F","orderId":"ORD-DEMO","amount":31.99,"currency":"USD","status":"DECLINED","declineReason":"CARD_DECLINED","processedAt":"2026-10-28T08:28:37+05:30"}
```
Response 503 (outage switched on):
```json
{"error":"PAYMENT_GATEWAY_UNAVAILABLE","message":"Simulated payment gateway outage"}
```
Inventory (worker → mock): `GET /api/inventory/P-500?quantity=5` →
`{"productId":"P-500","requested":5,"onHand":3,"available":false,"warehouse":"WH-SYNTH-01"}`

## 5. Process-variable dictionary

| Variable | Type | Set by | Purpose |
|---|---|---|---|
| orderId | String | start | Business key, idempotency key |
| customerRef | String | start | Synthetic customer reference |
| customerCategory | String | start / validate | NEW, REGULAR, GOLD, PLATINUM (defaults to NEW). DMN input |
| productId | String | start | Catalogue product (P-100 … P-500) |
| quantity | Integer | start / validate | 1–20 |
| deliveryPreference | String | start | STANDARD, EXPRESS, SAME_DAY. DMN input |
| paymentToken | String | start | Mock token `tok_test_*` (never a real card) |
| shippingPostcode | String | start | Information only |
| orderValid | Boolean | validate worker | Gateway *Order valid?* |
| validationErrors | List&lt;String&gt; | validate worker | Validation messages |
| productName, unitPrice, currency | String, Number, String | validate worker | Catalogue data |
| stockAvailable | Boolean | inventory worker | Gateway *In stock?* |
| stockOnHand, warehouse | Number, String | inventory worker | Stock details |
| subtotal, shippingFee, taxAmount | Number | total worker | Price breakdown |
| orderTotal | Number | total worker | DMN input, payment amount |
| priorityResult | Context | DMN task | Full DMN output |
| deliveryPriority | String | DMN task | CRITICAL / HIGH / MEDIUM / LOW |
| manualVerificationRequired | Boolean | DMN task | Gateway *Manual verification required?* |
| priorityReason | String | DMN task | Matched rule |
| verificationDecision | String | user task form | APPROVED / REJECTED (gateway *Verified?*) |
| verificationNotes | String | user task form | Reviewer notes |
| paymentId | String | REST connector | Gateway transaction id |
| paymentStatus | String | REST connector | APPROVED / DECLINED (gateway *Payment approved?*) |
| paymentDeclineReason | String | REST connector | CARD_DECLINED / LIMIT_EXCEEDED |
| reservationId, shipmentId, plannedDispatchDate | String | fulfilment worker | Fulfilment details |
| orderStatus | String | notify worker | FULFILMENT_SCHEDULED, CANCELLED_OUT_OF_STOCK, CANCELLED_BY_VERIFICATION, PAYMENT_DECLINED, REJECTED_INVALID |
| customerNotified, notifiedAt | Boolean, String | notify worker | Simulated notification audit |

## 6. Test cases and results

Executed on Camunda 8.8.44 (Camunda 8 Run, local) on 2026-10-28.

| TC | Input | Expected | Actual | Status |
|---|---|---|---|---|
| 01 | P-200 ×2, REGULAR, EXPRESS, `tok_test_visa_ok` | In stock → P6 MEDIUM → payment APPROVED → fulfilled | Total 209.37, MEDIUM, APPROVED, shipment created, `FULFILMENT_SCHEDULED` (2251799813686307) | PASS |
| 02 | P-500 ×5 (3 on hand) | *Insufficient stock* → not fulfilled | `stockAvailable=false`, `CANCELLED_OUT_OF_STOCK`. Payment and fulfilment never reached (2251799813686336) | PASS |
| 03 | P-100 ×1, `tok_test_declined` | Payment DECLINED → not fulfilled | `paymentStatus=DECLINED`, `CARD_DECLINED`, `PAYMENT_DECLINED` (2251799813686363) | PASS |
| 04 | P-400 ×3, GOLD, SAME_DAY | Total > 5000 → P1 → verification task → approved → paid → fulfilled | Total 6182.75, task in Tasklist; approved → payment APPROVED → fulfilled, dispatch +1 day (HIGH) (2251799813686421) | PASS |
| 05 | P-300 ×7, NEW | P2 → verification task → rejected | Total 2487.24, task rejected → `CANCELLED_BY_VERIFICATION` (2251799813686471) | PASS |
| 06 | P-300 ×1, PLATINUM, SAME_DAY + payment gateway down | Connector fails → incident → fix → retry → fulfilled | Incident `JOB_NO_RETRIES` on *Process payment* (HTTP 503 `PAYMENT_GATEWAY_UNAVAILABLE`). Gateway restored, incident retried → APPROVED → fulfilled, CRITICAL (2251799813686886) | PASS |
| 07 | P-100 ×3 + inventory service down | Worker fails → incident → fix → retry → fulfilled | Incident `JOB_NO_RETRIES` on *Check inventory* (`503 Service Unavailable`). Service restored, incident retried → fulfilled (2251799813687207) | PASS |
| 08 | `paymentToken = "pm_card_visa"` | Validation fails → not fulfilled | `orderValid=false`, "paymentToken must be a mock test token…", `REJECTED_INVALID` (2251799813686494) | PASS |

Unit tests: `mvn test` – 7 tests (validation, token check, pricing, shipping rule, status resolution), all passing.

## 7. Incident investigation and recovery

**Payment service failure (TC06)**
1. Operate shows an incident on **Process payment (REST)**, type *Job: No retries left*. The error message contains the HTTP response: status 503, body `PAYMENT_GATEWAY_UNAVAILABLE`.
2. Diagnosis: the payment gateway is down. The request itself is correct: the variables `orderTotal`, `paymentToken` and `currency` are valid.
3. Corrective action: bring the gateway back (`scripts/fix-services.ps1` → `PUT /api/mock/payments/available/true`).
4. **Retry** the incident in Operate → the connector calls the gateway again → APPROVED → fulfilment.

**Inventory service failure (TC07)**
1. Incident on **Check inventory**: `HttpServerErrorException$ServiceUnavailable: 503` thrown by the worker.
2. Corrective action: restore the inventory service (`fix-services.ps1`).
3. **Retry** → the worker runs again → stock available → the order completes.

## 8. Assumptions

- Prices are in USD from a synthetic 5-product catalogue. Tax is a flat 8 %. Standard shipping is free from 100.
- `customerCategory` defaults to NEW when it is not given.
- High-value threshold 5 000, and 2 000 for NEW customers. Gateway limit 10 000 (declines above it).
- A declined payment is a business outcome (200 + `DECLINED`), not a technical error. Only 5xx or no answer causes retries and an incident.

## 9. Limitations

- Stock and payment state are in memory. They reset when the app restarts, and repeated runs use up stock: reset it with `PUT /api/mock/inventory/stock/{productId}/{qty}`.
- Stock is checked first and reserved later, after payment. A real system would hold a reservation in between.
- No compensation (refund) step if fulfilment fails after a successful payment.
- On SaaS, the REST Connector needs a public tunnel to reach the local mock gateway.

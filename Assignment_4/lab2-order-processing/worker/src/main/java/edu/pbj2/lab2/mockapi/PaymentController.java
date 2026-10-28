package edu.pbj2.lab2.mockapi;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mock payment gateway called by the REST Connector. Accepts only {@code tok_test_*} tokens.
 * <ul>
 *   <li>{@code tok_test_declined} - returns status DECLINED (card declined),</li>
 *   <li>amount above 10,000 - returns status DECLINED (limit exceeded),</li>
 *   <li>{@code PUT /api/mock/payments/available/false} - every call returns HTTP 503.</li>
 * </ul>
 * Repeated calls with the same Idempotency-Key return the first result (safe connector retries).
 */
@RestController
public class PaymentController {

  private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
  private static final BigDecimal LIMIT = new BigDecimal("10000");

  private final AtomicBoolean serviceAvailable = new AtomicBoolean(true);
  private final Map<String, Map<String, Object>> processed = new ConcurrentHashMap<>();

  @PostMapping("/api/payments")
  public ResponseEntity<Map<String, Object>> pay(@RequestBody Map<String, Object> request,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    log.info("payment <- key={} {}", idempotencyKey, request);
    if (!serviceAvailable.get()) {
      log.warn("payment -> 503 (simulated gateway outage)");
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("error", "PAYMENT_GATEWAY_UNAVAILABLE", "message", "Simulated payment gateway outage"));
    }
    String token = String.valueOf(request.get("paymentToken"));
    if (!token.startsWith("tok_test_")) {
      return ResponseEntity.badRequest().body(Map.of("error", "INVALID_TOKEN", "message", "Only tok_test_* tokens are accepted"));
    }
    if (idempotencyKey != null && processed.containsKey(idempotencyKey)) {
      return ResponseEntity.ok(processed.get(idempotencyKey));
    }

    BigDecimal amount = new BigDecimal(String.valueOf(request.get("amount")));
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("paymentId", "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    body.put("orderId", request.get("orderId"));
    body.put("amount", amount);
    body.put("currency", request.get("currency"));
    if (token.equals("tok_test_declined")) {
      body.put("status", "DECLINED");
      body.put("declineReason", "CARD_DECLINED");
    } else if (amount.compareTo(LIMIT) > 0) {
      body.put("status", "DECLINED");
      body.put("declineReason", "LIMIT_EXCEEDED");
    } else {
      body.put("status", "APPROVED");
      body.put("declineReason", null);
    }
    body.put("processedAt", OffsetDateTime.now().toString());
    if (idempotencyKey != null) processed.put(idempotencyKey, body);
    log.info("payment -> 200 {}", body);
    return ResponseEntity.ok(body);
  }

  @PutMapping("/api/mock/payments/available/{available}")
  public Map<String, Object> setAvailable(@PathVariable boolean available) {
    serviceAvailable.set(available);
    log.warn("payment gateway available = {}", available);
    return Map.of("available", available);
  }
}

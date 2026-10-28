package edu.pbj2.lab1.mockapi;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mock credit-assessment API (synthetic data only) called by the REST Connector.
 *
 * <p>Failure simulation:
 * <ul>
 *   <li>applicantId starting with {@code SYN-ERR} returns HTTP 503,</li>
 *   <li>applicantId starting with {@code SYN-TIMEOUT} answers after the connector read timeout,</li>
 *   <li>{@code PUT /api/mock/credit/mode/{NORMAL|ERROR|TIMEOUT}} switches the behaviour for all calls.</li>
 * </ul>
 */
@RestController
public class CreditAssessmentController {

  private static final Logger log = LoggerFactory.getLogger(CreditAssessmentController.class);

  public enum Mode { NORMAL, ERROR, TIMEOUT }

  private final SyntheticCreditBureau bureau;
  private final long timeoutDelayMs;
  private final AtomicReference<Mode> mode = new AtomicReference<>(Mode.NORMAL);

  public CreditAssessmentController(SyntheticCreditBureau bureau,
      @Value("${lab.mock.timeout-delay-ms}") long timeoutDelayMs) {
    this.bureau = bureau;
    this.timeoutDelayMs = timeoutDelayMs;
  }

  @PostMapping("/api/credit/assessments")
  public ResponseEntity<Map<String, Object>> assess(@RequestBody Map<String, Object> request,
      @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) throws InterruptedException {
    String applicantId = String.valueOf(request.get("applicantId"));
    log.info("credit API <- {} applicant={} mode={}", correlationId, applicantId, mode.get());

    if (mode.get() == Mode.ERROR || applicantId.startsWith("SYN-ERR")) {
      log.warn("credit API -> 503 (simulated outage) for {}", applicantId);
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("error", "CREDIT_BUREAU_UNAVAILABLE", "message", "Simulated credit bureau outage"));
    }
    if (mode.get() == Mode.TIMEOUT || applicantId.startsWith("SYN-TIMEOUT")) {
      log.warn("credit API sleeping {} ms (simulated timeout) for {}", timeoutDelayMs, applicantId);
      Thread.sleep(timeoutDelayMs);
    }

    int score = bureau.scoreFor(applicantId);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("assessmentId", "CA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    body.put("applicantId", applicantId);
    body.put("creditScore", score);
    body.put("riskBand", SyntheticCreditBureau.riskBand(score));
    body.put("existingDefaults", bureau.defaultsFor(applicantId));
    body.put("assessedAt", OffsetDateTime.now().toString());
    log.info("credit API -> 200 {}", body);
    return ResponseEntity.ok(body);
  }

  @PutMapping("/api/mock/credit/mode/{newMode}")
  public Map<String, Object> setMode(@PathVariable Mode newMode) {
    log.warn("credit API mode changed {} -> {}", mode.getAndSet(newMode), newMode);
    return Map.of("mode", newMode);
  }

  @GetMapping("/api/mock/credit/mode")
  public Map<String, Object> getMode() {
    return Map.of("mode", mode.get());
  }
}

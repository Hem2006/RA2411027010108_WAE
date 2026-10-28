package edu.pbj2.lab2.mockapi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Simulated inventory service with synthetic stock levels.
 * {@code PUT /api/mock/inventory/available/false} makes every call return HTTP 503.
 */
@RestController
public class InventoryController {

  private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

  private final Map<String, Integer> stock = new ConcurrentHashMap<>(Map.of(
      "P-100", 500, "P-200", 40, "P-300", 12, "P-400", 8, "P-500", 3));
  private final AtomicBoolean serviceAvailable = new AtomicBoolean(true);

  @GetMapping("/api/inventory/{productId}")
  public Map<String, Object> check(@PathVariable String productId, @RequestParam int quantity) {
    ensureAvailable();
    int onHand = stock.getOrDefault(productId, 0);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("productId", productId);
    body.put("requested", quantity);
    body.put("onHand", onHand);
    body.put("available", onHand >= quantity);
    body.put("warehouse", "WH-SYNTH-01");
    log.info("inventory check -> {}", body);
    return body;
  }

  @PostMapping("/api/inventory/{productId}/reservations")
  public Map<String, Object> reserve(@PathVariable String productId, @RequestBody Map<String, Object> request) {
    ensureAvailable();
    int quantity = ((Number) request.get("quantity")).intValue();
    int remaining = stock.merge(productId, -quantity, Integer::sum);
    log.info("reserved {} x{} for {} (remaining {})", productId, quantity, request.get("orderId"), remaining);
    return Map.of("reservationId", "RSV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
        "remaining", remaining);
  }

  @PutMapping("/api/mock/inventory/available/{available}")
  public Map<String, Object> setAvailable(@PathVariable boolean available) {
    serviceAvailable.set(available);
    log.warn("inventory service available = {}", available);
    return Map.of("available", available);
  }

  @PutMapping("/api/mock/inventory/stock/{productId}/{quantity}")
  public Map<String, Object> setStock(@PathVariable String productId, @PathVariable int quantity) {
    stock.put(productId, quantity);
    return Map.of("productId", productId, "onHand", quantity);
  }

  private void ensureAvailable() {
    if (!serviceAvailable.get()) {
      log.warn("inventory service -> 503 (simulated outage)");
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Simulated inventory outage");
    }
  }
}

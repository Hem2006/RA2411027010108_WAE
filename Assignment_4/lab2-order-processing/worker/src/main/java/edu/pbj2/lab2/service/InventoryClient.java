package edu.pbj2.lab2.service;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP client for the simulated inventory service. Any HTTP error or connection problem is thrown
 * as an exception, which makes the calling job worker fail the job.
 */
@Component
public class InventoryClient {

  private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP = new ParameterizedTypeReference<>() {};

  private final RestClient http;

  public InventoryClient(RestClient.Builder builder, @Value("${lab.inventory.base-url}") String baseUrl) {
    this.http = builder.baseUrl(baseUrl).build();
  }

  public Map<String, Object> checkStock(String productId, int quantity) {
    return http.get()
        .uri("/api/inventory/{productId}?quantity={quantity}", productId, quantity)
        .retrieve()
        .body(JSON_MAP);
  }

  public Map<String, Object> reserve(String productId, int quantity, String orderId) {
    return http.post()
        .uri("/api/inventory/{productId}/reservations", productId)
        .body(Map.of("quantity", quantity, "orderId", orderId))
        .retrieve()
        .body(JSON_MAP);
  }
}

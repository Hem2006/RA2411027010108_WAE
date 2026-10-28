package edu.pbj2.lab3.service;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** HTTP client used by the booking worker. HTTP errors are thrown and fail the job. */
@Component
public class SchedulingClient {

  private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP = new ParameterizedTypeReference<>() {};

  private final RestClient http;

  public SchedulingClient(RestClient.Builder builder, @Value("${lab.scheduling.base-url}") String baseUrl) {
    this.http = builder.baseUrl(baseUrl).build();
  }

  public Map<String, Object> book(String slotId, String patientRef, String requestId) {
    return http.post()
        .uri("/api/scheduling/bookings")
        .body(Map.of("slotId", slotId, "patientRef", patientRef, "requestId", requestId))
        .retrieve()
        .body(JSON_MAP);
  }
}

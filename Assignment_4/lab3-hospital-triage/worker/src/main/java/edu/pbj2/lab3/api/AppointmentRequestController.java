package edu.pbj2.lab3.api;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Starts a hospital-triage process instance with the posted request as process variables. */
@RestController
public class AppointmentRequestController {

  private static final Logger log = LoggerFactory.getLogger(AppointmentRequestController.class);
  static final String PROCESS_ID = "hospital-triage-process";

  private final CamundaClient client;

  public AppointmentRequestController(CamundaClient client) {
    this.client = client;
  }

  @PostMapping("/api/appointment-requests")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> submit(@RequestBody Map<String, Object> request) {
    Map<String, Object> variables = new LinkedHashMap<>(request);
    variables.putIfAbsent("requestId", "AR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

    ProcessInstanceEvent instance = client.newCreateInstanceCommand()
        .bpmnProcessId(PROCESS_ID)
        .latestVersion()
        .variables(variables)
        .send()
        .join();

    log.info("started {} instance {} for request {}", PROCESS_ID, instance.getProcessInstanceKey(), variables.get("requestId"));
    return Map.of(
        "processInstanceKey", instance.getProcessInstanceKey(),
        "processDefinitionVersion", instance.getVersion(),
        "requestId", variables.get("requestId"));
  }
}

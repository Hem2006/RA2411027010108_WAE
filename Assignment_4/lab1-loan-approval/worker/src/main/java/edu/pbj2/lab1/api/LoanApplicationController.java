package edu.pbj2.lab1.api;

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

/** Starts a loan-approval process instance with the posted application as process variables. */
@RestController
public class LoanApplicationController {

  private static final Logger log = LoggerFactory.getLogger(LoanApplicationController.class);
  static final String PROCESS_ID = "loan-approval-process";

  private final CamundaClient client;

  public LoanApplicationController(CamundaClient client) {
    this.client = client;
  }

  @PostMapping("/api/loan-applications")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> submit(@RequestBody Map<String, Object> application) {
    Map<String, Object> variables = new LinkedHashMap<>(application);
    variables.putIfAbsent("applicationId", "LA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

    ProcessInstanceEvent instance = client.newCreateInstanceCommand()
        .bpmnProcessId(PROCESS_ID)
        .latestVersion()
        .variables(variables)
        .send()
        .join();

    log.info("started {} instance {} for application {}", PROCESS_ID,
        instance.getProcessInstanceKey(), variables.get("applicationId"));
    return Map.of(
        "processInstanceKey", instance.getProcessInstanceKey(),
        "processDefinitionVersion", instance.getVersion(),
        "applicationId", variables.get("applicationId"));
  }
}

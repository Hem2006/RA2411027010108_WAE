package edu.pbj2.lab2.api;

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

/** Starts an order-processing process instance with the posted order as process variables. */
@RestController
public class OrderController {

  private static final Logger log = LoggerFactory.getLogger(OrderController.class);
  static final String PROCESS_ID = "order-processing-process";

  private final CamundaClient client;

  public OrderController(CamundaClient client) {
    this.client = client;
  }

  @PostMapping("/api/orders")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> placeOrder(@RequestBody Map<String, Object> order) {
    Map<String, Object> variables = new LinkedHashMap<>(order);
    variables.putIfAbsent("orderId", "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

    ProcessInstanceEvent instance = client.newCreateInstanceCommand()
        .bpmnProcessId(PROCESS_ID)
        .latestVersion()
        .variables(variables)
        .send()
        .join();

    log.info("started {} instance {} for order {}", PROCESS_ID, instance.getProcessInstanceKey(), variables.get("orderId"));
    return Map.of(
        "processInstanceKey", instance.getProcessInstanceKey(),
        "processDefinitionVersion", instance.getVersion(),
        "orderId", variables.get("orderId"));
  }
}

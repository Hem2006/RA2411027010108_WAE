package edu.pbj2.lab2.worker;

import edu.pbj2.lab2.service.InventoryClient;
import edu.pbj2.lab2.service.OrderPricing;
import edu.pbj2.lab2.service.OrderValidator;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Job workers for validating the order, checking stock and calculating the total. */
@Component
public class OrderWorkers {

  private static final Logger log = LoggerFactory.getLogger(OrderWorkers.class);

  private final OrderValidator validator;
  private final InventoryClient inventory;
  private final OrderPricing pricing;

  public OrderWorkers(OrderValidator validator, InventoryClient inventory, OrderPricing pricing) {
    this.validator = validator;
    this.inventory = inventory;
    this.pricing = pricing;
  }

  @JobWorker(type = "lab2-validate-order")
  public Map<String, Object> validateOrder(ActivatedJob job) {
    Map<String, Object> result = validator.validate(job.getVariablesAsMap());
    log.info("[{}] order {} valid={} errors={}", job.getProcessInstanceKey(),
        job.getVariable("orderId"), result.get("orderValid"), result.get("validationErrors"));
    return result;
  }

  /**
   * Calls the inventory service. If it is unavailable the exception fails the job; once the
   * retries are used up Zeebe raises an incident that is resolved in Operate after the service
   * is back.
   */
  @JobWorker(type = "lab2-check-inventory")
  public Map<String, Object> checkInventory(ActivatedJob job) {
    String productId = (String) job.getVariable("productId");
    int quantity = ((Number) job.getVariable("quantity")).intValue();
    log.info("[{}] checking stock for {} x{} (retries left {})",
        job.getProcessInstanceKey(), productId, quantity, job.getRetries());

    Map<String, Object> stock = inventory.checkStock(productId, quantity);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("stockAvailable", stock.get("available"));
    result.put("stockOnHand", stock.get("onHand"));
    result.put("warehouse", stock.get("warehouse"));
    log.info("[{}] stock result {}", job.getProcessInstanceKey(), result);
    return result;
  }

  @JobWorker(type = "lab2-calculate-total")
  public Map<String, Object> calculateTotal(ActivatedJob job) {
    BigDecimal unitPrice = new BigDecimal(job.getVariable("unitPrice").toString());
    int quantity = ((Number) job.getVariable("quantity")).intValue();
    String delivery = (String) job.getVariable("deliveryPreference");

    Map<String, Object> result = pricing.calculate(unitPrice, quantity, delivery);
    log.info("[{}] order total {}", job.getProcessInstanceKey(), result);
    return result;
  }
}

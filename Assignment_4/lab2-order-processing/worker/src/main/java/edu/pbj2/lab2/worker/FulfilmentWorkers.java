package edu.pbj2.lab2.worker;

import edu.pbj2.lab2.service.InventoryClient;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Job workers for fulfilment and the final customer notification. */
@Component
public class FulfilmentWorkers {

  private static final Logger log = LoggerFactory.getLogger(FulfilmentWorkers.class);

  private final InventoryClient inventory;

  public FulfilmentWorkers(InventoryClient inventory) {
    this.inventory = inventory;
  }

  /** Reserves the stock and creates a (simulated) shipment whose dispatch date depends on priority. */
  @JobWorker(type = "lab2-arrange-fulfilment")
  public Map<String, Object> arrangeFulfilment(ActivatedJob job) {
    Map<String, Object> vars = job.getVariablesAsMap();
    String orderId = (String) vars.get("orderId");
    String priority = (String) vars.get("deliveryPriority");

    Map<String, Object> reservation = inventory.reserve(
        (String) vars.get("productId"), ((Number) vars.get("quantity")).intValue(), orderId);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("reservationId", reservation.get("reservationId"));
    result.put("shipmentId", "SHP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    result.put("plannedDispatchDate", LocalDate.now().plusDays(dispatchDelayDays(priority)).toString());
    log.info("[{}] fulfilment for {} ({}): {}", job.getProcessInstanceKey(), orderId, priority, result);
    return result;
  }

  @JobWorker(type = "lab2-notify-customer")
  public Map<String, Object> notifyCustomer(ActivatedJob job) {
    Map<String, Object> vars = job.getVariablesAsMap();
    String status = resolveOrderStatus(vars);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("orderStatus", status);
    result.put("customerNotified", true);
    result.put("notifiedAt", OffsetDateTime.now().toString());
    log.info("[{}] NOTIFY customer {}: order {} -> {}", job.getProcessInstanceKey(),
        vars.get("customerRef"), vars.get("orderId"), status);
    return result;
  }

  static int dispatchDelayDays(String priority) {
    if (priority == null) return 3;
    return switch (priority) {
      case "CRITICAL" -> 0;
      case "HIGH" -> 1;
      case "MEDIUM" -> 2;
      default -> 3;
    };
  }

  /** The order status follows from which path the instance took through the process. */
  static String resolveOrderStatus(Map<String, Object> vars) {
    if (Boolean.FALSE.equals(vars.get("orderValid"))) return "REJECTED_INVALID";
    if (Boolean.FALSE.equals(vars.get("stockAvailable"))) return "CANCELLED_OUT_OF_STOCK";
    if ("REJECTED".equals(vars.get("verificationDecision"))) return "CANCELLED_BY_VERIFICATION";
    if ("DECLINED".equals(vars.get("paymentStatus"))) return "PAYMENT_DECLINED";
    if (vars.get("shipmentId") != null) return "FULFILMENT_SCHEDULED";
    return "UNKNOWN";
  }
}

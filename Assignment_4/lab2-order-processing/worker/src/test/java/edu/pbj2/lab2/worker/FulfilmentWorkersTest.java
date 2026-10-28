package edu.pbj2.lab2.worker;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class FulfilmentWorkersTest {

  @Test
  void orderStatusFollowsTheProcessPath() {
    assertThat(FulfilmentWorkers.resolveOrderStatus(Map.of("orderValid", false))).isEqualTo("REJECTED_INVALID");
    assertThat(FulfilmentWorkers.resolveOrderStatus(Map.of("orderValid", true, "stockAvailable", false)))
        .isEqualTo("CANCELLED_OUT_OF_STOCK");
    assertThat(FulfilmentWorkers.resolveOrderStatus(Map.of("orderValid", true, "stockAvailable", true,
        "verificationDecision", "REJECTED"))).isEqualTo("CANCELLED_BY_VERIFICATION");
    assertThat(FulfilmentWorkers.resolveOrderStatus(Map.of("orderValid", true, "stockAvailable", true,
        "paymentStatus", "DECLINED"))).isEqualTo("PAYMENT_DECLINED");
    assertThat(FulfilmentWorkers.resolveOrderStatus(Map.of("orderValid", true, "stockAvailable", true,
        "paymentStatus", "APPROVED", "shipmentId", "SHP-1"))).isEqualTo("FULFILMENT_SCHEDULED");
  }

  @Test
  void dispatchDelayDependsOnPriority() {
    assertThat(FulfilmentWorkers.dispatchDelayDays("CRITICAL")).isZero();
    assertThat(FulfilmentWorkers.dispatchDelayDays("LOW")).isEqualTo(3);
  }
}

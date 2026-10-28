package edu.pbj2.lab2.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OrderServicesTest {

  private final OrderValidator validator = new OrderValidator(new ProductCatalog());
  private final OrderPricing pricing = new OrderPricing();

  private static Map<String, Object> order() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("orderId", "ORD-TEST");
    vars.put("customerRef", "CUST-0001");
    vars.put("customerCategory", "REGULAR");
    vars.put("productId", "P-200");
    vars.put("quantity", 2);
    vars.put("deliveryPreference", "EXPRESS");
    vars.put("paymentToken", "tok_test_visa_ok");
    return vars;
  }

  @Test
  void validOrderPasses() {
    Map<String, Object> result = validator.validate(order());
    assertThat(result.get("orderValid")).isEqualTo(true);
    assertThat(result.get("productName")).isEqualTo("Mechanical Keyboard");
  }

  @Test
  void realLookingCardNumberIsRefused() {
    Map<String, Object> vars = order();
    vars.put("paymentToken", "4111111111111111");
    Map<String, Object> result = validator.validate(vars);
    assertThat(result.get("orderValid")).isEqualTo(false);
    assertThat(result.get("validationErrors").toString()).contains("mock test token");
  }

  @Test
  void unknownProductAndBadQuantityAreReported() {
    Map<String, Object> vars = order();
    vars.put("productId", "P-999");
    vars.put("quantity", 0);
    assertThat((List<?>) validator.validate(vars).get("validationErrors")).hasSize(2);
  }

  @Test
  void totalIncludesShippingAndTax() {
    Map<String, Object> result = pricing.calculate(new BigDecimal("89.99"), 2, "EXPRESS");
    assertThat(result.get("subtotal")).isEqualTo(new BigDecimal("179.98"));
    assertThat(result.get("shippingFee")).isEqualTo(new BigDecimal("14.99"));
    assertThat(result.get("taxAmount")).isEqualTo(new BigDecimal("14.40"));
    assertThat(result.get("orderTotal")).isEqualTo(new BigDecimal("209.37"));
  }

  @Test
  void standardShippingIsFreeFrom100() {
    assertThat(OrderPricing.shippingFee("STANDARD", new BigDecimal("99.99"))).isEqualTo(new BigDecimal("4.99"));
    assertThat(OrderPricing.shippingFee("STANDARD", new BigDecimal("100.00"))).isEqualTo(new BigDecimal("0.00"));
  }
}

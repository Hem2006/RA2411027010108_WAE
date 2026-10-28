package edu.pbj2.lab2.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Order total = subtotal + shipping fee + 8% tax on the subtotal. */
@Service
public class OrderPricing {

  static final BigDecimal TAX_RATE = new BigDecimal("0.08");
  static final BigDecimal FREE_STANDARD_SHIPPING_FROM = new BigDecimal("100.00");

  public Map<String, Object> calculate(BigDecimal unitPrice, int quantity, String deliveryPreference) {
    BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    BigDecimal shipping = shippingFee(deliveryPreference, subtotal);
    BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
    BigDecimal total = subtotal.add(shipping).add(tax);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("subtotal", subtotal);
    result.put("shippingFee", shipping);
    result.put("taxAmount", tax);
    result.put("orderTotal", total);
    return result;
  }

  static BigDecimal shippingFee(String deliveryPreference, BigDecimal subtotal) {
    return switch (deliveryPreference) {
      case "SAME_DAY" -> new BigDecimal("29.99");
      case "EXPRESS" -> new BigDecimal("14.99");
      default -> subtotal.compareTo(FREE_STANDARD_SHIPPING_FROM) >= 0 ? new BigDecimal("0.00") : new BigDecimal("4.99");
    };
  }
}

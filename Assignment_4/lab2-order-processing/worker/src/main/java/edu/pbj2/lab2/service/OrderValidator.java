package edu.pbj2.lab2.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Business validation of an incoming order. Problems are returned as {@code orderValid=false}
 * with a list of errors; nothing here fails the job.
 */
@Service
public class OrderValidator {

  public static final Set<String> DELIVERY_PREFERENCES = Set.of("STANDARD", "EXPRESS", "SAME_DAY");
  public static final Set<String> CUSTOMER_CATEGORIES = Set.of("NEW", "REGULAR", "GOLD", "PLATINUM");
  public static final String TEST_TOKEN_PREFIX = "tok_test_";

  private final ProductCatalog catalog;

  public OrderValidator(ProductCatalog catalog) {
    this.catalog = catalog;
  }

  public Map<String, Object> validate(Map<String, Object> vars) {
    List<String> errors = new ArrayList<>();

    if (isBlank(vars.get("customerRef"))) errors.add("customerRef is required");

    String productId = String.valueOf(vars.get("productId"));
    Optional<ProductCatalog.Product> product = catalog.find(productId);
    if (product.isEmpty()) errors.add("unknown productId '" + productId + "'");

    Integer quantity = asInteger(vars.get("quantity"));
    if (quantity == null) errors.add("quantity must be a whole number");
    else if (quantity < 1 || quantity > 20) errors.add("quantity must be between 1 and 20");

    Object delivery = vars.get("deliveryPreference");
    if (delivery == null || !DELIVERY_PREFERENCES.contains(delivery.toString())) {
      errors.add("deliveryPreference must be one of " + DELIVERY_PREFERENCES);
    }

    String category = vars.get("customerCategory") == null ? "NEW" : vars.get("customerCategory").toString();
    if (!CUSTOMER_CATEGORIES.contains(category)) errors.add("customerCategory must be one of " + CUSTOMER_CATEGORIES);

    // Only mock tokens are accepted. Anything else (e.g. a card number) is refused, so no real
    // payment data can ever reach the mock gateway or the process variables of later steps.
    Object token = vars.get("paymentToken");
    if (token == null || !token.toString().startsWith(TEST_TOKEN_PREFIX)) {
      errors.add("paymentToken must be a mock test token starting with '" + TEST_TOKEN_PREFIX + "'");
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("orderValid", errors.isEmpty());
    result.put("validationErrors", errors);
    result.put("customerCategory", category);
    result.put("currency", "USD");
    if (quantity != null) result.put("quantity", quantity);
    product.ifPresent(p -> {
      result.put("productName", p.name());
      result.put("unitPrice", p.unitPrice());
    });
    return result;
  }

  private static Integer asInteger(Object value) {
    if (value instanceof Number n && n.doubleValue() == Math.floor(n.doubleValue())) return n.intValue();
    if (value == null) return null;
    try {
      return Integer.parseInt(value.toString().trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static boolean isBlank(Object value) {
    return value == null || value.toString().isBlank();
  }
}

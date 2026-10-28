package edu.pbj2.lab2.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Synthetic product catalogue (prices in USD). */
@Component
public class ProductCatalog {

  public record Product(String productId, String name, BigDecimal unitPrice) {}

  private static final Map<String, Product> PRODUCTS = Map.of(
      "P-100", new Product("P-100", "Wireless Mouse", new BigDecimal("25.00")),
      "P-200", new Product("P-200", "Mechanical Keyboard", new BigDecimal("89.99")),
      "P-300", new Product("P-300", "27in Monitor", new BigDecimal("329.00")),
      "P-400", new Product("P-400", "Laptop Pro 14", new BigDecimal("1899.00")),
      "P-500", new Product("P-500", "Studio Camera Kit", new BigDecimal("2750.00")));

  public Optional<Product> find(String productId) {
    return Optional.ofNullable(PRODUCTS.get(productId));
  }
}

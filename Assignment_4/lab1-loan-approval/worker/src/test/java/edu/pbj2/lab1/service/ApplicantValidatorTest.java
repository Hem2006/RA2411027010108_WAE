package edu.pbj2.lab1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ApplicantValidatorTest {

  private final ApplicantValidator validator = new ApplicantValidator();

  private static Map<String, Object> validApplication() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("applicationId", "LA-TEST");
    vars.put("applicantId", "SYN-APP-1001");
    vars.put("applicantName", "Asha Synthetic");
    vars.put("age", 34);
    vars.put("email", "asha.synthetic@example.com");
    vars.put("loanAmount", 20000);
    vars.put("loanTermMonths", 48);
    vars.put("monthlyIncome", 6500);
    vars.put("monthlyDebt", 400);
    vars.put("employmentStatus", "EMPLOYED");
    return vars;
  }

  @Test
  void validApplicationPassesAndComputesRatios() {
    Map<String, Object> result = validator.validate(validApplication());

    assertThat(result.get("validationPassed")).isEqualTo(true);
    assertThat((List<?>) result.get("validationErrors")).isEmpty();
    assertThat((double) result.get("estimatedMonthlyInstallment")).isBetween(497.0, 498.0);
    assertThat((double) result.get("debtToIncomeRatio")).isEqualTo(0.14);
    assertThat((double) result.get("loanToIncomeRatio")).isEqualTo(0.26);
  }

  @Test
  void underageApplicantFailsValidationWithoutException() {
    Map<String, Object> vars = validApplication();
    vars.put("age", 17);

    Map<String, Object> result = validator.validate(vars);

    assertThat(result.get("validationPassed")).isEqualTo(false);
    assertThat(result.get("validationErrors")).isEqualTo(List.of("applicant must be at least 18 years old"));
  }

  @Test
  void nonNumericIncomeIsATechnicalFailure() {
    Map<String, Object> vars = validApplication();
    vars.put("monthlyIncome", "six thousand");

    assertThatThrownBy(() -> validator.validate(vars))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("monthlyIncome must be numeric");
  }

  @Test
  void numericStringIsAcceptedAndNormalised() {
    Map<String, Object> vars = validApplication();
    vars.put("monthlyIncome", "6500");

    assertThat(validator.validate(vars).get("monthlyIncome")).isEqualTo(6500.0);
  }
}

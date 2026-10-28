package edu.pbj2.lab1.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Validates a loan application and derives the ratios the DMN table needs.
 *
 * <p>Two kinds of problems are handled differently on purpose:
 * <ul>
 *   <li>Business-rule problems (age under 18, unknown employment status, ...) are returned as
 *       {@code validationPassed=false} so the process can route the application to rejection.</li>
 *   <li>Technical problems (a numeric field that cannot be read as a number) throw
 *       {@link IllegalArgumentException}. The job worker then fails the job, Zeebe decrements the
 *       retries and finally raises an incident that can be fixed and retried in Operate.</li>
 * </ul>
 */
@Service
public class ApplicantValidator {

  public static final Set<String> EMPLOYMENT_STATUSES =
      Set.of("EMPLOYED", "SELF_EMPLOYED", "RETIRED", "STUDENT", "UNEMPLOYED");

  private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
  private static final double ANNUAL_INTEREST_RATE = 0.09;

  public Map<String, Object> validate(Map<String, Object> vars) {
    double monthlyIncome = requireNumber(vars, "monthlyIncome");
    double loanAmount = requireNumber(vars, "loanAmount");
    double monthlyDebt = optionalNumber(vars, "monthlyDebt", 0);
    double loanTermMonths = optionalNumber(vars, "loanTermMonths", 36);
    double age = optionalNumber(vars, "age", -1);

    List<String> errors = new ArrayList<>();
    if (isBlank(vars.get("applicantName"))) errors.add("applicantName is required");
    if (isBlank(vars.get("applicantId"))) errors.add("applicantId is required");
    if (age < 0) errors.add("age is required");
    else if (age < 18) errors.add("applicant must be at least 18 years old");
    else if (age > 75) errors.add("applicant must be 75 years old or younger");
    Object email = vars.get("email");
    if (email != null && !EMAIL.matcher(email.toString()).matches()) errors.add("email format is invalid");
    if (loanAmount < 1_000 || loanAmount > 250_000) errors.add("loanAmount must be between 1,000 and 250,000");
    if (loanTermMonths < 6 || loanTermMonths > 84) errors.add("loanTermMonths must be between 6 and 84");
    if (monthlyIncome <= 0) errors.add("monthlyIncome must be greater than 0");
    if (monthlyDebt < 0) errors.add("monthlyDebt cannot be negative");
    Object employment = vars.get("employmentStatus");
    if (employment == null || !EMPLOYMENT_STATUSES.contains(employment.toString())) {
      errors.add("employmentStatus must be one of " + EMPLOYMENT_STATUSES);
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("validationPassed", errors.isEmpty());
    result.put("validationErrors", errors);
    // Store the numbers back as real numbers, so a corrected string value is normalised too.
    result.put("monthlyIncome", monthlyIncome);
    result.put("loanAmount", loanAmount);
    result.put("monthlyDebt", monthlyDebt);
    result.put("loanTermMonths", (int) loanTermMonths);
    if (monthlyIncome > 0) {
      double installment = monthlyInstallment(loanAmount, (int) Math.max(loanTermMonths, 1));
      result.put("estimatedMonthlyInstallment", round(installment));
      result.put("debtToIncomeRatio", round((monthlyDebt + installment) / monthlyIncome));
      result.put("loanToIncomeRatio", round(loanAmount / (monthlyIncome * 12)));
    }
    return result;
  }

  static double monthlyInstallment(double principal, int months) {
    double r = ANNUAL_INTEREST_RATE / 12;
    return principal * r / (1 - Math.pow(1 + r, -months));
  }

  private static double requireNumber(Map<String, Object> vars, String name) {
    Object value = vars.get(name);
    if (value == null) {
      throw new IllegalArgumentException(name + " is missing");
    }
    return toNumber(name, value);
  }

  private static double optionalNumber(Map<String, Object> vars, String name, double fallback) {
    Object value = vars.get(name);
    return value == null ? fallback : toNumber(name, value);
  }

  private static double toNumber(String name, Object value) {
    if (value instanceof Number n) return n.doubleValue();
    try {
      return Double.parseDouble(value.toString().trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          name + " must be numeric but was '" + value + "'. Fix the variable in Operate and retry the job.");
    }
  }

  private static boolean isBlank(Object value) {
    return value == null || value.toString().isBlank();
  }

  private static double round(double value) {
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
  }
}

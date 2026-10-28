package edu.pbj2.lab3.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Validates the registration fields and normalises the urgency indicators for the DMN table
 * (missing booleans become {@code false}, missing numbers become 0).
 */
@Service
public class RegistrationValidator {

  /** Only synthetic patient references are accepted - no names or real identifiers. */
  public static final Pattern SYNTHETIC_PATIENT_REF = Pattern.compile("^SYN-PAT-\\d{4}$");
  public static final Set<String> DEPARTMENTS =
      Set.of("GENERAL_MEDICINE", "CARDIOLOGY", "DERMATOLOGY", "ORTHOPAEDICS", "PAEDIATRICS");
  public static final Set<String> TIMES_OF_DAY = Set.of("MORNING", "AFTERNOON", "ANY");
  private static final List<String> FLAGS = List.of("chestPain", "breathingDifficulty", "severeBleeding", "highFever");

  private final Clock clock;

  public RegistrationValidator() {
    this(Clock.systemDefaultZone());
  }

  RegistrationValidator(Clock clock) {
    this.clock = clock;
  }

  public Map<String, Object> validate(Map<String, Object> vars) {
    List<String> errors = new ArrayList<>();
    Map<String, Object> result = new LinkedHashMap<>();

    Object patientRef = vars.get("patientRef");
    if (patientRef == null || !SYNTHETIC_PATIENT_REF.matcher(patientRef.toString()).matches()) {
      errors.add("patientRef must be a synthetic reference like SYN-PAT-0001");
    }

    Double age = number(vars.get("patientAge"));
    if (age == null || age < 0 || age > 120) errors.add("patientAge must be between 0 and 120");

    Object department = vars.get("department");
    if (department == null || !DEPARTMENTS.contains(department.toString())) {
      errors.add("department must be one of " + DEPARTMENTS);
    }

    Object timeOfDay = vars.getOrDefault("preferredTimeOfDay", "ANY");
    if (!TIMES_OF_DAY.contains(timeOfDay.toString())) errors.add("preferredTimeOfDay must be one of " + TIMES_OF_DAY);
    result.put("preferredTimeOfDay", timeOfDay);

    LocalDate today = LocalDate.now(clock);
    try {
      LocalDate preferred = LocalDate.parse(String.valueOf(vars.get("preferredDate")));
      if (preferred.isBefore(today)) errors.add("preferredDate cannot be in the past");
      else if (preferred.isAfter(today.plusDays(90))) errors.add("preferredDate must be within 90 days");
    } catch (DateTimeParseException e) {
      errors.add("preferredDate must be an ISO date (yyyy-MM-dd)");
    }

    Object symptoms = vars.get("symptomsSummary");
    if (symptoms == null || symptoms.toString().isBlank()) errors.add("symptomsSummary is required");
    else if (symptoms.toString().length() > 500) errors.add("symptomsSummary must be 500 characters or fewer");

    Double pain = number(vars.getOrDefault("painScore", 0));
    if (pain == null || pain < 0 || pain > 10) errors.add("painScore must be between 0 and 10");
    else result.put("painScore", pain.intValue());

    Double duration = number(vars.getOrDefault("symptomDurationDays", 0));
    if (duration == null || duration < 0) errors.add("symptomDurationDays must be 0 or more");
    else result.put("symptomDurationDays", duration.intValue());

    for (String flag : FLAGS) {
      result.put(flag, Boolean.TRUE.equals(vars.get(flag)) || "true".equalsIgnoreCase(String.valueOf(vars.get(flag))));
    }

    result.put("registrationValid", errors.isEmpty());
    result.put("registrationErrors", errors);
    return result;
  }

  private static Double number(Object value) {
    if (value instanceof Number n) return n.doubleValue();
    try {
      return value == null ? null : Double.valueOf(value.toString().trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }
}

package edu.pbj2.lab3;

import static org.assertj.core.api.Assertions.assertThat;

import edu.pbj2.lab3.mockapi.SchedulingControllerTestAccess;
import edu.pbj2.lab3.service.RegistrationValidatorTestAccess;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class Lab3LogicTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 11, 2); // a Monday

  private static Map<String, Object> request() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("requestId", "AR-TEST");
    vars.put("patientRef", "SYN-PAT-0001");
    vars.put("patientAge", 42);
    vars.put("department", "GENERAL_MEDICINE");
    vars.put("preferredDate", TODAY.plusDays(1).toString());
    vars.put("preferredTimeOfDay", "MORNING");
    vars.put("symptomsSummary", "Synthetic: mild sore throat");
    vars.put("painScore", 2);
    return vars;
  }

  @Test
  void validRequestNormalisesIndicators() {
    Map<String, Object> result = RegistrationValidatorTestAccess.validate(TODAY, request());
    assertThat(result.get("registrationValid")).isEqualTo(true);
    assertThat(result.get("chestPain")).isEqualTo(false);
    assertThat(result.get("symptomDurationDays")).isEqualTo(0);
  }

  @Test
  void realLookingPatientIdentifierIsRejected() {
    Map<String, Object> vars = request();
    vars.put("patientRef", "John Smith");
    Map<String, Object> result = RegistrationValidatorTestAccess.validate(TODAY, vars);
    assertThat(result.get("registrationValid")).isEqualTo(false);
  }

  @Test
  void pastDateIsRejected() {
    Map<String, Object> vars = request();
    vars.put("preferredDate", TODAY.minusDays(1).toString());
    assertThat(RegistrationValidatorTestAccess.validate(TODAY, vars).get("registrationErrors").toString())
        .contains("past");
  }

  @Test
  void slotsRespectLeadTimeWindowAndFullyBookedDepartment() {
    assertThat(SchedulingControllerTestAccess.slots("GENERAL_MEDICINE", TODAY, "MORNING", 14)).hasSize(5);
    assertThat(SchedulingControllerTestAccess.slots("DERMATOLOGY", TODAY, "ANY", 14)).isEmpty();
    assertThat(SchedulingControllerTestAccess.slots("ORTHOPAEDICS", TODAY, "ANY", 3)).isEmpty();
    assertThat(SchedulingControllerTestAccess.slots("ORTHOPAEDICS", TODAY, "ANY", 14)).isNotEmpty();
  }
}

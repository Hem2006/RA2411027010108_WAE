package edu.pbj2.lab3.worker;

import edu.pbj2.lab3.service.RegistrationValidator;
import edu.pbj2.lab3.service.SchedulingClient;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Job workers for registration validation, appointment booking and patient notification. */
@Component
public class TriageWorkers {

  private static final Logger log = LoggerFactory.getLogger(TriageWorkers.class);

  private final RegistrationValidator validator;
  private final SchedulingClient scheduling;

  public TriageWorkers(RegistrationValidator validator, SchedulingClient scheduling) {
    this.validator = validator;
    this.scheduling = scheduling;
  }

  @JobWorker(type = "lab3-validate-registration")
  public Map<String, Object> validateRegistration(ActivatedJob job) {
    Map<String, Object> result = validator.validate(job.getVariablesAsMap());
    log.info("[{}] request {} patient {} valid={} errors={}", job.getProcessInstanceKey(),
        job.getVariable("requestId"), job.getVariable("patientRef"),
        result.get("registrationValid"), result.get("registrationErrors"));
    return result;
  }

  /**
   * Requests the first available slot returned by the REST connector. If the scheduling API is
   * down the exception fails the job and, after the retries, raises an incident.
   */
  @JobWorker(type = "lab3-book-appointment")
  public Map<String, Object> bookAppointment(ActivatedJob job) {
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> slots = (List<Map<String, Object>>) job.getVariable("availableSlots");
    Map<String, Object> slot = slots.getFirst();
    String patientRef = (String) job.getVariable("patientRef");
    log.info("[{}] booking slot {} for {} (retries left {})",
        job.getProcessInstanceKey(), slot.get("slotId"), patientRef, job.getRetries());

    Map<String, Object> booking = scheduling.book(
        (String) slot.get("slotId"), patientRef, (String) job.getVariable("requestId"));

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("bookingId", booking.get("bookingId"));
    result.put("bookingStatus", booking.get("status"));
    result.put("bookedSlot", slot);
    log.info("[{}] booking result {}", job.getProcessInstanceKey(), result);
    return result;
  }

  @JobWorker(type = "lab3-notify-patient")
  public Map<String, Object> notifyPatient(ActivatedJob job) {
    Map<String, Object> vars = job.getVariablesAsMap();
    String status = resolveRequestStatus(vars);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("requestStatus", status);
    result.put("patientNotified", true);
    result.put("notificationChannel", "SMS (simulated)");
    result.put("notifiedAt", OffsetDateTime.now().toString());
    log.info("[{}] NOTIFY patient {}: request {} -> {}", job.getProcessInstanceKey(),
        vars.get("patientRef"), vars.get("requestId"), status);
    return result;
  }

  /** The request status follows from which path the instance took through the process. */
  static String resolveRequestStatus(Map<String, Object> vars) {
    if (Boolean.FALSE.equals(vars.get("registrationValid"))) return "REGISTRATION_REJECTED";
    if (vars.get("assessmentOutcome") != null) return "URGENT_ASSESSED_" + vars.get("assessmentOutcome");
    if ("CONFIRMED".equals(vars.get("bookingStatus"))) return "APPOINTMENT_BOOKED";
    if (vars.get("schedulingOutcome") != null) return "MANUAL_" + vars.get("schedulingOutcome");
    return "UNKNOWN";
  }
}

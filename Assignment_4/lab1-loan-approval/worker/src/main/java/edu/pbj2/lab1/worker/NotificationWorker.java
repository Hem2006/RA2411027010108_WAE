package edu.pbj2.lab1.worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Job type {@code lab1-notify-applicant}. Works out the final outcome from the variables produced
 * by earlier activities and "sends" a notification (logged only, no real e-mail is sent).
 */
@Component
public class NotificationWorker {

  private static final Logger log = LoggerFactory.getLogger(NotificationWorker.class);

  @JobWorker(type = "lab1-notify-applicant")
  public Map<String, Object> notifyApplicant(ActivatedJob job) {
    Map<String, Object> vars = job.getVariablesAsMap();
    Outcome outcome = resolveOutcome(vars);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("finalStatus", outcome.status());
    result.put("finalReason", outcome.reason());
    result.put("decidedBy", outcome.decidedBy());
    result.put("notificationSent", true);
    result.put("notificationChannel", "EMAIL (simulated)");
    result.put("notifiedAt", OffsetDateTime.now().toString());

    log.info("[{}] NOTIFY {} <{}>: application {} is {} ({}; decided by {})",
        job.getProcessInstanceKey(), vars.get("applicantName"), vars.get("email"),
        vars.get("applicationId"), outcome.status(), outcome.reason(), outcome.decidedBy());
    return result;
  }

  static Outcome resolveOutcome(Map<String, Object> vars) {
    if (Boolean.FALSE.equals(vars.get("validationPassed"))) {
      return new Outcome("REJECTED", "Invalid application: " + vars.get("validationErrors"), "VALIDATION");
    }
    Object reviewDecision = vars.get("reviewDecision");
    if (reviewDecision != null) {
      return new Outcome(reviewDecision.toString(), String.valueOf(vars.get("reviewComments")), "LOAN_OFFICER");
    }
    Object loanDecision = vars.get("loanDecision");
    if (loanDecision != null) {
      return new Outcome(loanDecision.toString(), String.valueOf(vars.get("decisionReason")), "DMN");
    }
    return new Outcome("REJECTED", "No decision available", "SYSTEM");
  }

  record Outcome(String status, String reason, String decidedBy) {}
}

package edu.pbj2.lab1.worker;

import edu.pbj2.lab1.service.ApplicantValidator;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Job type {@code lab1-validate-applicant}: "Validate applicant" service task. */
@Component
public class ApplicantValidationWorker {

  private static final Logger log = LoggerFactory.getLogger(ApplicantValidationWorker.class);

  private final ApplicantValidator validator;

  public ApplicantValidationWorker(ApplicantValidator validator) {
    this.validator = validator;
  }

  /**
   * Returned variables are merged into the process instance. A thrown exception fails the job
   * (retries - 1); when the retries reach 0 Zeebe creates an incident.
   */
  @JobWorker(type = "lab1-validate-applicant")
  public Map<String, Object> validate(ActivatedJob job) {
    Map<String, Object> vars = job.getVariablesAsMap();
    log.info("[{}] validating application {} (job {}, retries left {})",
        job.getProcessInstanceKey(), vars.get("applicationId"), job.getKey(), job.getRetries());
    try {
      Map<String, Object> result = validator.validate(vars);
      log.info("[{}] validationPassed={} errors={} dti={} lti={}", job.getProcessInstanceKey(),
          result.get("validationPassed"), result.get("validationErrors"),
          result.get("debtToIncomeRatio"), result.get("loanToIncomeRatio"));
      return result;
    } catch (IllegalArgumentException e) {
      log.error("[{}] technical validation failure: {}", job.getProcessInstanceKey(), e.getMessage());
      throw e;
    }
  }
}

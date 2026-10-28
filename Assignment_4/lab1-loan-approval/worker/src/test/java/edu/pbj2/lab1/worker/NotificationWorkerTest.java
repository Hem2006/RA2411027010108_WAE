package edu.pbj2.lab1.worker;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NotificationWorkerTest {

  @Test
  void invalidApplicationIsRejectedByValidation() {
    var outcome = NotificationWorker.resolveOutcome(
        Map.of("validationPassed", false, "validationErrors", List.of("age")));
    assertThat(outcome.status()).isEqualTo("REJECTED");
    assertThat(outcome.decidedBy()).isEqualTo("VALIDATION");
  }

  @Test
  void reviewerDecisionOverridesDmn() {
    var outcome = NotificationWorker.resolveOutcome(Map.of("validationPassed", true,
        "loanDecision", "MANUAL_REVIEW", "reviewDecision", "APPROVED", "reviewComments", "ok"));
    assertThat(outcome.status()).isEqualTo("APPROVED");
    assertThat(outcome.decidedBy()).isEqualTo("LOAN_OFFICER");
  }

  @Test
  void dmnDecisionIsUsedWithoutReview() {
    var outcome = NotificationWorker.resolveOutcome(Map.of("validationPassed", true,
        "loanDecision", "REJECTED", "decisionReason", "R2"));
    assertThat(outcome.status()).isEqualTo("REJECTED");
    assertThat(outcome.decidedBy()).isEqualTo("DMN");
  }
}

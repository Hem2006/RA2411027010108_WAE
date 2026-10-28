package edu.pbj2.lab1.mockapi;

import java.util.Map;
import org.springframework.stereotype.Component;

/** Fixed synthetic credit records. No real people or real credit data. */
@Component
public class SyntheticCreditBureau {

  private static final Map<String, Integer> SCORES = Map.of(
      "SYN-APP-1001", 782,
      "SYN-APP-1002", 515,
      "SYN-APP-1003", 664,
      "SYN-APP-1004", 735,
      "SYN-APP-1005", 701);

  private static final Map<String, Integer> DEFAULTS = Map.of("SYN-APP-1002", 2);

  /** Unknown applicants get a deterministic "thin file" score between 600 and 699. */
  public int scoreFor(String applicantId) {
    return SCORES.getOrDefault(applicantId, 600 + Math.floorMod(applicantId.hashCode(), 100));
  }

  public int defaultsFor(String applicantId) {
    return DEFAULTS.getOrDefault(applicantId, 0);
  }

  public static String riskBand(int score) {
    if (score >= 750) return "LOW";
    if (score >= 650) return "MEDIUM";
    if (score >= 550) return "HIGH";
    return "VERY_HIGH";
  }
}

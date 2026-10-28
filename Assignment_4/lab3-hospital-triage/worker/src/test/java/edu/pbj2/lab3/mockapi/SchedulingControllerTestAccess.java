package edu.pbj2.lab3.mockapi;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class SchedulingControllerTestAccess {

  private SchedulingControllerTestAccess() {}

  public static List<Map<String, Object>> slots(String department, LocalDate from, String timeOfDay, int withinDays) {
    return new SchedulingController().generateSlots(department, from, timeOfDay, withinDays);
  }
}

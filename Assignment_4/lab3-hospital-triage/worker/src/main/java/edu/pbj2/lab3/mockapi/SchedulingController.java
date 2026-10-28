package edu.pbj2.lab3.mockapi;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Mock hospital scheduling API with generated synthetic slots.
 * <ul>
 *   <li>Each department has a lead time: GENERAL_MEDICINE/PAEDIATRICS 1 day, CARDIOLOGY 3,
 *       ORTHOPAEDICS 5; DERMATOLOGY is fully booked.</li>
 *   <li>Slots are on weekdays at 09:00, 10:30 (MORNING) and 14:00, 15:30 (AFTERNOON).</li>
 *   <li>Only slots within {@code withinDays} of {@code fromDate} are returned (max 5).</li>
 *   <li>{@code PUT /api/mock/scheduling/available/false} makes every call return HTTP 503.</li>
 * </ul>
 */
@RestController
public class SchedulingController {

  private static final Logger log = LoggerFactory.getLogger(SchedulingController.class);
  private static final DateTimeFormatter SLOT_ID = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");
  private static final Map<String, Integer> LEAD_DAYS =
      Map.of("GENERAL_MEDICINE", 1, "PAEDIATRICS", 1, "CARDIOLOGY", 3, "ORTHOPAEDICS", 5);
  private static final List<LocalTime> MORNING = List.of(LocalTime.of(9, 0), LocalTime.of(10, 30));
  private static final List<LocalTime> AFTERNOON = List.of(LocalTime.of(14, 0), LocalTime.of(15, 30));

  private final AtomicBoolean serviceAvailable = new AtomicBoolean(true);
  private final Set<String> bookedSlots = ConcurrentHashMap.newKeySet();

  @GetMapping("/api/scheduling/slots")
  public Map<String, Object> slots(@RequestParam String department, @RequestParam String fromDate,
      @RequestParam(defaultValue = "ANY") String timeOfDay, @RequestParam(defaultValue = "14") int withinDays) {
    ensureAvailable();
    List<Map<String, Object>> slots = generateSlots(department, LocalDate.parse(fromDate), timeOfDay, withinDays);
    log.info("slots <- {} from {} {} within {}d -> {} slot(s)", department, fromDate, timeOfDay, withinDays, slots.size());
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("department", department);
    body.put("fromDate", fromDate);
    body.put("withinDays", withinDays);
    body.put("slots", slots);
    return body;
  }

  @PostMapping("/api/scheduling/bookings")
  public Map<String, Object> book(@RequestBody Map<String, Object> request) {
    ensureAvailable();
    String slotId = String.valueOf(request.get("slotId"));
    if (!bookedSlots.add(slotId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot " + slotId + " is already booked");
    }
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("bookingId", "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    body.put("slotId", slotId);
    body.put("patientRef", request.get("patientRef"));
    body.put("status", "CONFIRMED");
    log.info("booking -> {}", body);
    return body;
  }

  @PutMapping("/api/mock/scheduling/available/{available}")
  public Map<String, Object> setAvailable(@PathVariable boolean available) {
    serviceAvailable.set(available);
    log.warn("scheduling API available = {}", available);
    return Map.of("available", available);
  }

  List<Map<String, Object>> generateSlots(String department, LocalDate fromDate, String timeOfDay, int withinDays) {
    List<Map<String, Object>> slots = new ArrayList<>();
    Integer lead = LEAD_DAYS.get(department);
    if (lead == null) return slots;

    List<LocalTime> times = switch (timeOfDay) {
      case "MORNING" -> MORNING;
      case "AFTERNOON" -> AFTERNOON;
      default -> List.of(MORNING.get(0), MORNING.get(1), AFTERNOON.get(0), AFTERNOON.get(1));
    };
    for (int offset = lead; offset <= withinDays && slots.size() < 5; offset++) {
      LocalDate day = fromDate.plusDays(offset);
      if (day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
      for (LocalTime time : times) {
        String slotId = department.substring(0, 3) + "-" + day.atTime(time).format(SLOT_ID);
        if (bookedSlots.contains(slotId) || slots.size() >= 5) continue;
        Map<String, Object> slot = new LinkedHashMap<>();
        slot.put("slotId", slotId);
        slot.put("start", day.atTime(time).toString());
        slot.put("clinician", "Dr Synthetic " + (char) ('A' + Math.floorMod(slotId.hashCode(), 6)));
        slots.add(slot);
      }
    }
    return slots;
  }

  private void ensureAvailable() {
    if (!serviceAvailable.get()) {
      log.warn("scheduling API -> 503 (simulated outage)");
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Simulated scheduling outage");
    }
  }
}

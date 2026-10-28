package edu.pbj2.lab3.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

public final class RegistrationValidatorTestAccess {

  private RegistrationValidatorTestAccess() {}

  public static Map<String, Object> validate(LocalDate today, Map<String, Object> vars) {
    Clock clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    return new RegistrationValidator(clock).validate(vars);
  }
}

package edu.pbj2.lab3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Lab 3 - hospital appointment and triage (synthetic patients only). One Spring Boot app that hosts:
 * <ul>
 *   <li>the external job workers (package {@code worker}),</li>
 *   <li>the mock hospital scheduling API (package {@code mockapi}),</li>
 *   <li>a small REST endpoint that starts process instances (package {@code api}).</li>
 * </ul>
 */
@SpringBootApplication
public class Lab3Application {

  public static void main(String[] args) {
    SpringApplication.run(Lab3Application.class, args);
  }
}

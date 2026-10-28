package edu.pbj2.lab2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Lab 2 - e-commerce order processing. One Spring Boot app that hosts:
 * <ul>
 *   <li>the external job workers (package {@code worker}),</li>
 *   <li>the simulated inventory service and mock payment gateway (package {@code mockapi}),</li>
 *   <li>a small REST endpoint that starts process instances (package {@code api}).</li>
 * </ul>
 */
@SpringBootApplication
public class Lab2Application {

  public static void main(String[] args) {
    SpringApplication.run(Lab2Application.class, args);
  }
}

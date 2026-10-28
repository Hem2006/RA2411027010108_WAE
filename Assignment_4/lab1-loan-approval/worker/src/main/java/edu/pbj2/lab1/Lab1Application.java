package edu.pbj2.lab1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Lab 1 - loan approval. One Spring Boot app that hosts:
 * <ul>
 *   <li>the external job workers (package {@code worker}),</li>
 *   <li>the mock credit-assessment REST API called by the REST Connector (package {@code mockapi}),</li>
 *   <li>a small REST endpoint that starts process instances (package {@code api}).</li>
 * </ul>
 */
@SpringBootApplication
public class Lab1Application {

  public static void main(String[] args) {
    SpringApplication.run(Lab1Application.class, args);
  }
}

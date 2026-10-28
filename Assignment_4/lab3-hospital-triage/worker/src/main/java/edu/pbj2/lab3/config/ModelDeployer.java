package edu.pbj2.lab3.config;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.DeployResourceCommandStep1.DeployResourceCommandStep2;
import io.camunda.client.api.response.DeploymentEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Deploys the BPMN, DMN and form files from the lab's models folder in a single deployment. */
@Component
@ConditionalOnProperty(name = "lab.deploy.enabled", havingValue = "true")
public class ModelDeployer implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(ModelDeployer.class);

  private final CamundaClient client;
  private final Path modelsDir;

  public ModelDeployer(CamundaClient client, @Value("${lab.deploy.models-dir}") String modelsDir) {
    this.client = client;
    this.modelsDir = Path.of(modelsDir).toAbsolutePath().normalize();
  }

  @Override
  public void run(ApplicationArguments args) throws IOException {
    List<Path> files;
    try (Stream<Path> stream = Files.list(modelsDir)) {
      files = stream
          .filter(p -> p.toString().matches(".*\\.(bpmn|dmn|form)$"))
          .sorted()
          .toList();
    }
    if (files.isEmpty()) {
      log.warn("No model files found in {}", modelsDir);
      return;
    }

    DeployResourceCommandStep2 command = null;
    for (Path file : files) {
      command = command == null
          ? client.newDeployResourceCommand().addResourceFile(file.toString())
          : command.addResourceFile(file.toString());
    }
    DeploymentEvent deployment = command.send().join();

    log.info("Deployment {} created from {}", deployment.getKey(), modelsDir);
    deployment.getProcesses().forEach(p ->
        log.info("  process  {} v{} (key {})", p.getBpmnProcessId(), p.getVersion(), p.getProcessDefinitionKey()));
    deployment.getDecisions().forEach(d ->
        log.info("  decision {} v{} (key {})", d.getDmnDecisionId(), d.getVersion(), d.getDecisionKey()));
    deployment.getForm().forEach(f ->
        log.info("  form     {} v{} (key {})", f.getFormId(), f.getVersion(), f.getFormKey()));
  }
}

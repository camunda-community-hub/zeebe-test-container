package io.zeebe.containers.test;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.zeebe.containers.ZeebeContainer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
final class ZeebeContainerEngineTest {
  @Container
  private final ZeebeContainer container =
      new ZeebeContainer()
          .withEnv(
              "ZEEBE_BROKER_EXPORTERS_DEBUG_CLASSNAME",
              "io.camunda.zeebe.broker.exporter.debug.DebugHttpExporter")
          .withEnv("ZEEBE_BROKER_EXPORTERS_DEBUG_ARGS_PORT", "8080")
          .withAdditionalExposedPort(8080);

  private final ZeebeContainerEngine engine = new ZeebeContainerEngine(container, 8080);

  @BeforeEach
  void beforeEach() {
    engine.start();
    BpmnAssert.initRecordStream(engine.getRecordStream());
  }

  @AfterEach
  void afterEach() {
    engine.stop();
    BpmnAssert.resetRecordStream();
  }

  @Test
  void shouldAssertSomething() throws InterruptedException {
    // given
    final String processId = "process";
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess(processId).startEvent().endEvent().done();
    final DeploymentEvent deploymentEvent;
    final ProcessInstanceEvent processInstanceEvent;

    // when
    try (final ZeebeClient client = engine.createClient()) {
      deploymentEvent =
          client.newDeployCommand().addProcessModel(process, "process.bpmn").send().join();
      processInstanceEvent =
          client.newCreateInstanceCommand().bpmnProcessId(processId).latestVersion().send().join();
    }

    // then
    BpmnAssert.assertThat(deploymentEvent).extractingProcessByBpmnProcessId(processId);
    Awaitility.await("until the process is completed")
        .pollInSameThread()
        .untilAsserted(() -> BpmnAssert.assertThat(processInstanceEvent).isCompleted());
  }
}

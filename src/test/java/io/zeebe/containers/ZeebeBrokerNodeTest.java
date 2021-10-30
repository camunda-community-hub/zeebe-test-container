/*
 * Copyright © 2019 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe.containers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.condition.OS.LINUX;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.camunda.zeebe.test.util.asserts.TopologyAssert;
import io.zeebe.containers.clock.ContainerClock;
import io.zeebe.containers.util.TestUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.agrona.CloseHelper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.okhttp3.OkHttpClient;
import org.testcontainers.shaded.okhttp3.Request.Builder;
import org.testcontainers.shaded.okhttp3.Response;

class ZeebeBrokerNodeTest {
  @SuppressWarnings("unused")
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  @ParameterizedTest(name = "{0} should be ready on start")
  @MethodSource("nodeProvider")
  void shouldBeReadyOnStart(final String testName, final ZeebeBrokerNode<?> node)
      throws IOException {
    // given
    final Response response;
    try (final GenericContainer<?> container = node.self()) {
      final OkHttpClient httpClient = new OkHttpClient();
      // when
      container.start();
      response =
          httpClient
              .newCall(
                  new Builder()
                      .get()
                      .url(String.format("http://%s/ready", node.getExternalMonitoringAddress()))
                      .build())
              .execute();
    }

    // then
    assertThat(response.code())
        .as("the broker ready check should return 204 when the container is started")
        .isEqualTo(204);
  }

  @SuppressWarnings("unused")
  @ParameterizedTest(name = "{0} should expose all ports except gateway")
  @MethodSource("nodeProvider")
  void shouldExposeAllPortsButGateway(final String testName, final ZeebeBrokerNode<?> node) {
    // given
    final List<Integer> expectedPorts =
        Arrays.stream(ZeebePort.values()).map(ZeebePort::getPort).collect(Collectors.toList());

    // when
    final List<Integer> exposedPorts = node.getExposedPorts();
    expectedPorts.remove((Integer) ZeebePort.GATEWAY.getPort());

    // then
    assertThat(exposedPorts)
        .as("the broker should expose all the ports but the gateway")
        .containsAll(expectedPorts);
  }

  @SuppressWarnings("unused")
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  @ParameterizedTest(name = "{0}")
  @MethodSource("reuseDataTestCases")
  @EnabledOnOs(LINUX)
  void shouldReuseHostDataOnRestart(
      final String testName,
      final BrokerNodeProvider brokerNodeProvider,
      final @TempDir Path dataDir) {
    // given
    final ZeebeBrokerNode<?> broker = brokerNodeProvider.apply(dataDir);
    final ZeebeGatewayContainer gateway =
        new ZeebeGatewayContainer()
            .withEnv("ZEEBE_GATEWAY_CLUSTER_CONTACTPOINT", broker.getInternalClusterAddress());

    // when
    broker.start();
    gateway.start();

    try (final ZeebeClient client = ZeebeClientFactory.newZeebeClient(gateway)) {
      // deploy a new process, which we can use on restart to assert that the data was correctly
      // reused
      final DeploymentEvent deployment = deploySampleProcess(client);
      broker.stop();

      // on restart we need to wait until the gateway is aware of the new leader
      broker.start();
      awaitUntilTopologyIsComplete(client);
      final ProcessInstanceEvent processInstance = createSampleProcessInstance(client);

      // then
      assertThat(processInstance)
          .as("the process instance was successfully created")
          .isNotNull()
          .extracting(ProcessInstanceEvent::getProcessDefinitionKey)
          .isEqualTo(deployment.getProcesses().get(0).getProcessDefinitionKey());
    } finally {
      CloseHelper.quietCloseAll(gateway, broker);
    }
  }

  @SuppressWarnings("unused")
  @ParameterizedTest(name = "{0} should fail to access the clock when not configured")
  @MethodSource("nodeProvider")
  @Execution(ExecutionMode.CONCURRENT)
  void shouldFailToGetClockIfNotConfigured(final String testName, final ZeebeBrokerNode<?> node) {
    // given - when - then
    assertThatCode(node::getClock).isInstanceOf(UnsupportedOperationException.class);
  }

  @SuppressWarnings("unused")
  @ParameterizedTest(name = "{0} should be able to access the clock when configured")
  @MethodSource("nodeProvider")
  @Execution(ExecutionMode.CONCURRENT)
  void shouldGetClockIfConfigured(final String testName, final ZeebeBrokerNode<?> node) {
    // given
    node.withContainerClockEnabled();

    // when
    final ContainerClock clock = node.getClock();

    // then
    assertThat(clock).isNotNull();
  }

  @SuppressWarnings({"unused", "java:S2925"})
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  @ParameterizedTest(name = "{0} should modify the container time via its clock")
  @MethodSource("nodeProvider")
  void shouldTimeTravel(final String testName, final ZeebeBrokerNode<?> node)
      throws IOException, InterruptedException {
    // given
    final Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    final GenericContainer<?> container = node.self();
    node.withContainerClockEnabled();

    // when
    final Instant containerTime;
    try {
      container.start();
      node.getClock().pinTime(now);
      Thread.sleep(1_000);
      containerTime = TestUtils.getContainerInstant(container);
    } finally {
      container.stop();
    }
    final ContainerClock clock = node.getClock();

    // then
    assertThat(containerTime).isBetween(now.minusSeconds(1), now.plusMillis(100));
  }

  private static ZeebeBrokerNode<?> provideBrokerWithHostData(
      final ZeebeBrokerNode<?> broker, final Path dataDir) {
    // configure the broker to use the same UID and GID as our current user so we can remove the
    // temporary directory at the end. Note that this is only necessary when not running the tests
    // as root
    final ZeebeHostData data = new ZeebeHostData(dataDir.toAbsolutePath().toString());
    final String runAsUser = TestUtils.getRunAsUser();
    broker
        .withZeebeData(data)
        .self()
        .withCreateContainerCmdModifier(cmd -> cmd.withUser(runAsUser));

    return broker;
  }

  private ProcessInstanceEvent createSampleProcessInstance(final ZeebeClient client) {
    return client.newCreateInstanceCommand().bpmnProcessId("process").latestVersion().send().join();
  }

  private DeploymentEvent deploySampleProcess(final ZeebeClient client) {
    final BpmnModelInstance sampleProcess =
        Bpmn.createExecutableProcess("process").startEvent().endEvent().done();
    return client.newDeployCommand().addProcessModel(sampleProcess, "process.bpmn").send().join();
  }

  private void awaitUntilTopologyIsComplete(final ZeebeClient client) {
    Awaitility.await("until topology is complete")
        .atMost(Duration.ofSeconds(30))
        .untilAsserted(
            () ->
                TopologyAssert.assertThat(client.newTopologyRequest().send().join())
                    .isComplete(1, 1));
  }

  private static Stream<Arguments> reuseDataTestCases() {
    return Stream.of(
        new ReuseDataTestCase(
            "broker with embedded gateway should reuse host data",
            path -> provideBrokerWithHostData(new ZeebeContainer(), path)),
        new ReuseDataTestCase(
            "broker without embedded gateway should reuse host data",
            path -> provideBrokerWithHostData(new ZeebeBrokerContainer(), path)),
        new ReuseDataTestCase(
            "broker with embedded gateway should reuse volume",
            path -> new ZeebeContainer().withZeebeData(ZeebeVolume.newVolume())),
        new ReuseDataTestCase(
            "broker without embedded gateway should reuse volume",
            path -> new ZeebeBrokerContainer().withZeebeData(ZeebeVolume.newVolume())));
  }

  private static Stream<Arguments> nodeProvider() {
    final Stream<ZeebeBrokerNode<?>> nodes =
        Stream.of(new ZeebeContainer(), new ZeebeBrokerContainer());
    return nodes.map(node -> Arguments.of(node.getClass().getSimpleName(), node));
  }

  private static final class ReuseDataTestCase implements Arguments {
    private final String testName;
    private final BrokerNodeProvider brokerProvider;

    public ReuseDataTestCase(final String testName, final BrokerNodeProvider brokerProvider) {
      this.testName = testName;
      this.brokerProvider = brokerProvider;
    }

    @Override
    public Object[] get() {
      return new Object[] {testName, brokerProvider};
    }
  }

  private interface BrokerNodeProvider extends Function<Path, ZeebeBrokerNode<?>> {}
}

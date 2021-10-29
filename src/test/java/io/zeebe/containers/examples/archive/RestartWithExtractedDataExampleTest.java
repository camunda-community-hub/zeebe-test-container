/*
 * Copyright © 2021 camunda services GmbH (info@camunda.com)
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
package io.zeebe.containers.examples.archive;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceResult;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.zeebe.containers.ZeebeContainer;
import io.zeebe.containers.ZeebeDefaults;
import io.zeebe.containers.ZeebeVolume;
import io.zeebe.containers.archive.ContainerArchive;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.utility.MountableFile;

/**
 * This example showcases how you could generate data from Zeebe, and use it as a starting point for
 * further tests, or even just download it locally to start a local broker from your IDE using this
 * data.
 *
 * <p>The example is divided into two parts. The first part generates the data in a container and
 * extracts the data to the local host. The second part then reuses that data and start a broker and
 * create a process instance from the previously deployed process.
 */
@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(OrderAnnotation.class)
final class RestartWithExtractedDataExampleTest {
  private static final String DATA_DIR = "generated";
  private static final String PROCESS_ID = "process";
  private static Path tempDir;

  @BeforeAll
  static void beforeAll(@TempDir final Path tempDir) {
    RestartWithExtractedDataExampleTest.tempDir = tempDir;
  }

  /**
   * Start a container with a volume, deploy a process, and extract the generated data to some
   * folder. In the next test, we will reuse this data to create a process instance, proving that
   * the data was successfully reused.
   *
   * <p>NOTE: since Zeebe is an asynchronous system, and we cannot guarantee when the engine is
   * idle, we stop the container before extracting the data. This causes us to start a second
   * (albeit tiny) container to extract the data.
   *
   * <p>If you all you wanted was to extract data from a live container, then you can use the {@link
   * ContainerArchive#builder()} directly and pass in your live container. For such a use case, you
   * wouldn't even need to be using a volume.
   */
  @Test
  @Order(1)
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void shouldGenerateData() throws IOException {
    // given
    final ZeebeVolume volume = ZeebeVolume.newVolume();
    final Path destination = tempDir.resolve(DATA_DIR);

    // when
    try (final ZeebeContainer container = new ZeebeContainer().withZeebeData(volume)) {
      container.start();
      deployProcess(container);
    }
    volume.extract(destination);

    // then
    assertThat(destination).isNotEmptyDirectory();
  }

  /**
   * The test here will start a process based on the definition deployed in the previous test.
   *
   * <p>The data here is copied to avoid further modifying it. We could also use a {@link
   * io.zeebe.containers.ZeebeHostData} if that was not a constraint.
   */
  @Test
  @Order(2)
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void shouldRestartWithGeneratedData() {
    // given
    final Path dataPath =
        Paths.get(
            tempDir.resolve(DATA_DIR).toString(), ZeebeDefaults.getInstance().getDefaultDataPath());

    // when
    try (final ZeebeContainer container =
        new ZeebeContainer()
            .withCopyFileToContainer(
                MountableFile.forHostPath(dataPath),
                ZeebeDefaults.getInstance().getDefaultDataPath())) {

      // when
      container.start();
      final ProcessInstanceResult result = createProcessInstance(container);

      // then
      assertThat(result).isNotNull();
    }
  }

  private void deployProcess(final ZeebeContainer container) {
    try (final ZeebeClient client =
        ZeebeClient.newClientBuilder()
            .usePlaintext()
            .gatewayAddress(container.getExternalGatewayAddress())
            .build()) {
      client
          .newDeployCommand()
          .addProcessModel(
              Bpmn.createExecutableProcess(PROCESS_ID).startEvent().endEvent().done(),
              "process.bpmn")
          .send()
          .join();
    }
  }

  private ProcessInstanceResult createProcessInstance(final ZeebeContainer container) {
    try (final ZeebeClient client =
        ZeebeClient.newClientBuilder()
            .usePlaintext()
            .gatewayAddress(container.getExternalGatewayAddress())
            .build()) {
      return client
          .newCreateInstanceCommand()
          .bpmnProcessId(PROCESS_ID)
          .latestVersion()
          .withResult()
          .send()
          .join();
    }
  }
}

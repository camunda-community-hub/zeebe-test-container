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
package io.zeebe.containers.examples;

import static org.assertj.core.api.Assertions.assertThat;

import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.response.WorkflowInstanceEvent;
import io.zeebe.containers.ZeebeContainer;
import io.zeebe.containers.ZeebeVolume;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * io.zeebe.containers.examples.cluster.ZeebeClusterWithEmbeddedGatewaysExampleTest This example
 * show cases how to create a simple test against a single node broker where data is kept across
 * restarts.
 *
 * <p>To validate this, we deploy a process, restart the node, and then create an instance. If the
 * data was not kept, then the original process wouldn't have been deployed.
 */
@Testcontainers
final class ReusableVolumeExampleTest {
  private final ZeebeVolume volume = ZeebeVolume.newVolume();

  @Container
  private final ZeebeContainer zeebeContainer = new ZeebeContainer().withZeebeData(volume);

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void shouldConnectToZeebe() {
    // given
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess("process").startEvent().endEvent().done();

    // when
    try (final ZeebeClient client = newZeebeClient(zeebeContainer)) {
      client.newDeployCommand().addWorkflowModel(process, "process.bpmn").send().join();
    }

    // restart
    zeebeContainer.stop();
    zeebeContainer.start();

    // create a process instance from the one we previously deployed - this would fail if we hadn't
    // previously deployed our process model
    final WorkflowInstanceEvent processInstance;
    try (final ZeebeClient client = newZeebeClient(zeebeContainer)) {
      processInstance =
          client.newCreateInstanceCommand().bpmnProcessId("process").latestVersion().send().join();
    }

    // then
    assertThat(processInstance.getWorkflowInstanceKey()).isPositive();
  }

  private ZeebeClient newZeebeClient(final ZeebeContainer node) {
    return ZeebeClient.newClientBuilder()
        .gatewayAddress(node.getExternalGatewayAddress())
        .usePlaintext()
        .build();
  }
}

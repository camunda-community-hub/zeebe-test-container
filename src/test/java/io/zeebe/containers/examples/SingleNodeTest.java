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

import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.response.DeploymentEvent;
import io.zeebe.client.api.response.WorkflowInstanceResult;
import io.zeebe.client.api.worker.JobWorker;
import io.zeebe.containers.ZeebeContainer;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * This example show cases how to create a simple test against a single node broker with embedded
 * gateway. A workflow is deployed, a new instance created, completed, and the result can then be
 * verified. In most cases, this is what you're looking for.
 */
@Testcontainers
class SingleNodeTest {
  @Container private final ZeebeContainer zeebeContainer = new ZeebeContainer();

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void shouldConnectToZeebe() {
    // given
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess("process")
            .startEvent()
            .serviceTask()
            .zeebeJobType("task")
            .endEvent()
            .done();
    final Map<String, Integer> variables = Maps.newHashMap("foo", 1);
    final DeploymentEvent deploymentEvent;
    final WorkflowInstanceResult workflowInstanceResult;

    // when
    try (final ZeebeClient client = newZeebeClient(zeebeContainer)) {
      final JobWorker worker =
          client
              .newWorker()
              .jobType("task")
              .handler(
                  (jobClient, job) ->
                      jobClient.newCompleteCommand(job.getKey()).variables(variables).send())
              .open();

      try {
        deploymentEvent =
            client.newDeployCommand().addWorkflowModel(process, "process.bpmn").send().join();
        workflowInstanceResult =
            client
                .newCreateInstanceCommand()
                .bpmnProcessId("process")
                .latestVersion()
                .withResult()
                .send()
                .join();
      } finally {
        worker.close();
      }
    }

    // then
    Assertions.assertThat(deploymentEvent.getWorkflows()).hasSize(1);
    Assertions.assertThat(workflowInstanceResult.getBpmnProcessId()).isEqualTo("process");
    Assertions.assertThat(workflowInstanceResult.getVariablesAsMap()).isEqualTo(variables);
  }

  private ZeebeClient newZeebeClient(final ZeebeContainer node) {
    return ZeebeClient.newClientBuilder()
        .gatewayAddress(node.getExternalGatewayAddress())
        .usePlaintext()
        .build();
  }
}

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
package io.zeebe.containers.examples;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.client.api.worker.JobWorker;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.containers.ZeebeContainer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * This example showcases how you can modify the time of a broker to trigger time events. Note that
 * changing the time will change the time for any program in the container. Typically, this should
 * be fine as you're only running Zeebe in your container, but if you were to execute any command
 * (or attach a shell) to the container, the time would also be modified accordingly.
 *
 * <p>See {@link io.zeebe.containers.clock.ContainerClock} for more possibilities.
 */
@Testcontainers
final class TimeTravelExampleTest {
  @Container
  private final ZeebeContainer container = new ZeebeContainer().withContainerClockEnabled();

  @SuppressWarnings("unused")
  @Test
  void shouldTriggerTimer() throws InterruptedException {
    // given
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess("process")
            .startEvent("timer")
            .timerWithDuration("PT10M")
            .serviceTask("task", b -> b.zeebeJobType("type"))
            .endEvent()
            .done();
    final List<ActivatedJob> jobs = Collections.synchronizedList(new ArrayList<>());
    final CountDownLatch barrier = new CountDownLatch(1);
    final JobHandler handler =
        (client, job) -> {
          client.newCompleteCommand(job.getKey());
          jobs.add(job);
          barrier.countDown();
        };

    // when
    try (final ZeebeClient client = newZeebeClient(container);
        final JobWorker worker = client.newWorker().jobType("type").handler(handler).open()) {
      client.newDeployCommand().addProcessModel(process, "process.bpmn").send().join();
      container.getClock().addTime(Duration.ofMinutes(10));
      assertThat(barrier.await(30, TimeUnit.SECONDS)).isTrue();
    }

    // then
    assertThat(jobs).hasSize(1);
  }

  private ZeebeClient newZeebeClient(final ZeebeContainer node) {
    return ZeebeClient.newClientBuilder()
        .gatewayAddress(node.getExternalGatewayAddress())
        .usePlaintext()
        .build();
  }
}

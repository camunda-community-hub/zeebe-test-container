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
package io.zeebe.containers.examples.clock;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.client.api.worker.JobWorker;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.containers.ZeebeContainer;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
final class FixedDateExampleTest {
  @Container
  private final ZeebeContainer container = new ZeebeContainer().withContainerClockEnabled();

  @SuppressWarnings("unused")
  @Test
  void shouldTriggerTimer() throws InterruptedException {
    // given
    final ZonedDateTime triggerTime = ZonedDateTime.now();
    final BpmnModelInstance process =
        Bpmn.createExecutableProcess("process")
            .startEvent("timer")
            .timerWithDate(triggerTime.toString())
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
      // we can set a fixed instant by pinning the time, and then unpin it so time keeps moving
      container.getClock().pinTime(triggerTime.toInstant());
      container.getClock().unpinTime();
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

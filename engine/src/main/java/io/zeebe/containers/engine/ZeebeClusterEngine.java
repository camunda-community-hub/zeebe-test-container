/*
 * Copyright © 2022 camunda services GmbH (info@camunda.com)
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
package io.zeebe.containers.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.ZeebeClientBuilder;
import io.camunda.zeebe.client.impl.ZeebeObjectMapper;
import io.camunda.zeebe.process.test.api.RecordStreamSource;
import io.zeebe.containers.ZeebeNode;
import io.zeebe.containers.clock.ZeebeClock;
import io.zeebe.containers.cluster.ZeebeCluster;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.UnaryOperator;
import org.agrona.CloseHelper;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/**
 * A {@link ContainerEngine} implementation which wraps a {@link ZeebeCluster}. Records are streamed
 * from all brokers/partitions to a single underlying receiver.
 *
 * <p>Manipulating the time will update the clock on all nodes more or less at the same time.
 */
@API(status = Status.INTERNAL)
final class ZeebeClusterEngine implements TestAwareContainerEngine {
  private final List<ZeebeClient> clients = new ArrayList<>();
  private final DebugReceiverStream recordStream;
  private final ZeebeCluster cluster;
  private final Collection<ZeebeClock> clocks;

  public ZeebeClusterEngine(final ZeebeCluster cluster, final DebugReceiverStream recordStream) {
    this.cluster = cluster;
    this.recordStream = recordStream;

    clocks = new ArrayList<>();
    for (final ZeebeNode<?> node : cluster.getNodes().values()) {
      node.withEnv("ZEEBE_CLOCK_CONTROLLED", "true");
      clocks.add(ZeebeClock.newDefaultClock(node));
    }
  }

  @Override
  public void acknowledge(final int partitionId, final long position) {
    recordStream.acknowledge(partitionId, position);
  }

  @Override
  public RecordStreamSource getRecordStreamSource() {
    return recordStream;
  }

  @Override
  public ZeebeClient createClient() {
    return createClient(UnaryOperator.identity());
  }

  @Override
  public ZeebeClient createClient(final ObjectMapper customObjectMapper) {
    return createClient(b -> b.withJsonMapper(new ZeebeObjectMapper(customObjectMapper)));
  }

  @Override
  public String getGatewayAddress() {
    return cluster.getAvailableGateway().getExternalGatewayAddress();
  }

  @Override
  public void increaseTime(final Duration timeToAdd) {
    clocks.forEach(clock -> clock.addTime(timeToAdd));
  }

  @Override
  public void waitForIdleState(final Duration timeout)
      throws InterruptedException, TimeoutException {
    recordStream.waitForIdleState(timeout);
  }

  @Override
  public void waitForBusyState(final Duration timeout)
      throws InterruptedException, TimeoutException {
    recordStream.waitForBusyState(timeout);
  }

  @Override
  public void start() {
    recordStream.start(cluster.getBrokers().values());
    cluster.start();
  }

  @Override
  public void stop() {
    CloseHelper.closeAll(clients);
    clients.clear();

    CloseHelper.closeAll(cluster, recordStream);
  }

  private ZeebeClient createClient(final UnaryOperator<ZeebeClientBuilder> configurator) {
    final ZeebeClientBuilder builder =
        configurator.apply(
            ZeebeClient.newClientBuilder().usePlaintext().gatewayAddress(getGatewayAddress()));
    final ZeebeClient client = builder.build();
    clients.add(client);

    return client;
  }
}

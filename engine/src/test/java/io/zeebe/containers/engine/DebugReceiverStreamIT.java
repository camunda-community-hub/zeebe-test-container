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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNoException;

import io.camunda.zeebe.protocol.record.ImmutableRecord;
import io.camunda.zeebe.protocol.record.Record;
import io.zeebe.containers.ZeebeBrokerContainer;
import io.zeebe.containers.ZeebeBrokerNode;
import io.zeebe.containers.ZeebeContainer;
import io.zeebe.containers.exporter.DebugReceiver;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

final class DebugReceiverStreamIT {
  private final InfiniteList<Record<?>> records = new InfiniteList<>();
  private final DebugReceiver receiver = new DebugReceiver(records::add);

  @Test
  void shouldConfigureBrokersOnStart() {
    // given
    final Collection<ZeebeBrokerNode<?>> brokers =
        Arrays.asList(new ZeebeBrokerContainer(), new ZeebeContainer());

    // when
    final int port;
    try (final DebugReceiverStream stream = new DebugReceiverStream(records, receiver)) {
      stream.start(brokers);
      port = receiver.serverAddress().getPort();
    }

    // then - we don't want to know too much about how the exporter is configured, so it should be
    // sufficient to only
    final String expectedExporterUrl = "http://host.testcontainers.internal:" + port + "/records";
    assertThat(brokers)
        .allSatisfy(
            broker ->
                assertThat(broker.getEnvMap())
                    .containsEntry("ZEEBE_BROKER_EXPORTERS_DEBUG_ARGS_URL", expectedExporterUrl));
  }

  @Test
  void shouldStartReceiverOnStart() {
    // given
    final Collection<ZeebeBrokerNode<?>> brokers = Collections.singleton(new ZeebeContainer());

    // when
    try (final DebugReceiverStream stream = new DebugReceiverStream(records, receiver)) {
      stream.start(brokers);
      // then
      assertThatNoException().isThrownBy(() -> testServerConnection(receiver.serverAddress()));
    }
  }

  @Test
  void shouldStopReceiverOnStop() {
    // given
    final Collection<ZeebeBrokerNode<?>> brokers = Collections.singleton(new ZeebeContainer());

    // when
    final InetSocketAddress serverAddress;
    try (final DebugReceiverStream stream = new DebugReceiverStream(records, receiver)) {
      stream.start(brokers);
      serverAddress = receiver.serverAddress();
    }

    // then
    assertThatCode(() -> testServerConnection(serverAddress)).isInstanceOf(ConnectException.class);
  }

  @Test
  void shouldWaitForIdleState() {
    // given

    // when
    try (final DebugReceiverStream stream = new DebugReceiverStream(records, receiver)) {
      // then
      assertThatNoException().isThrownBy(() -> stream.waitForIdleState(Duration.ofSeconds(2)));
    }
  }

  @Test
  void shouldTimeoutWaitingForIdleStateWhenNoRecords() {
    // given
    final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    // when
    try (final DebugReceiverStream stream = new DebugReceiverStream(records, receiver)) {
      executor.scheduleAtFixedRate(
          () -> records.add(ImmutableRecord.builder().build()), 100, 500, TimeUnit.MILLISECONDS);

      // then
      assertThatCode(() -> stream.waitForIdleState(Duration.ofSeconds(2)))
          .isInstanceOf(TimeoutException.class);
    }
  }

  @Test
  void shouldWaitForBusyState() {
    // given
    final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    // when
    try (final DebugReceiverStream stream = new DebugReceiverStream(records, receiver)) {
      executor.schedule(
          () -> records.add(ImmutableRecord.builder().build()), 500, TimeUnit.MILLISECONDS);

      // then
      assertThatNoException().isThrownBy(() -> stream.waitForIdleState(Duration.ofSeconds(2)));
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void shouldTimeOutWaitingForBusyState() {
    // given

    // when
    try (final DebugReceiverStream stream = new DebugReceiverStream(records, receiver)) {
      // then
      assertThatCode(() -> stream.waitForBusyState(Duration.ofSeconds(1)))
          .isInstanceOf(TimeoutException.class);
    }
  }

  private void testServerConnection(final InetSocketAddress receiverAddress) throws IOException {
    try (final Socket socket = new Socket()) {
      socket.connect(receiverAddress, 1_000);
    }
  }
}

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
package io.zeebe.containers.exporter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.camunda.zeebe.protocol.record.Record;
import io.restassured.RestAssured;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class DebugReceiverTest {
  private final ProtocolFactory recordFactory = new ProtocolFactory();

  /**
   * Iterates over all local ports from 1024 to 65535 until it finds a free port, and binds to it.
   * The bound socket is returned, and the caller should make sure to unbind it when needs be.
   */
  private ServerSocket bindUnusedPort() {
    // ports below 1024 are reserved on Unix systems
    for (int i = 1024; i < 65_536; i++) {
      try {
        final ServerSocket socket = new ServerSocket(i);
        socket.setReuseAddress(true);
        return socket;
      } catch (final IOException ignored) {
        // try again
      }
    }

    throw new IllegalStateException(
        "Could not find an unused port to bind localhost at between 1024 and 65535");
  }

  @Nested
  final class EndpointTest {
    @Test
    void shouldReturnServerAddressWhenStarted() {
      // given
      try (final DebugReceiver receiver = new DebugReceiver(r -> {})) {

        // when
        receiver.start();
        final InetSocketAddress serverAddress = (InetSocketAddress) receiver.serverAddress();

        // then
        assertThat(serverAddress.getHostName()).isEqualTo("localhost");
        assertThat(serverAddress.getPort()).isPositive();
      }
    }

    @Test
    void shouldNotReturnServerAddressWhenClosed() {
      // given
      final DebugReceiver receiver = new DebugReceiver(r -> {});

      // when - then
      assertThatCode(receiver::serverAddress).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldReturnRecordsEndpointWhenStarted() {
      // given
      try (final DebugReceiver receiver = new DebugReceiver(r -> {})) {

        // when
        receiver.start();
        final URI endpoint = receiver.recordsEndpoint();

        // then
        assertThat(endpoint).hasHost("127.0.0.1");
        assertThat(endpoint.getPort()).isPositive();
        assertThat(endpoint).hasPath("/records");
      }
    }

    @Test
    void shouldNotReturnRecordsEndpointWhenClosed() {
      // given
      final DebugReceiver receiver = new DebugReceiver(r -> {});

      // when - then
      assertThatCode(receiver::recordsEndpoint).isInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  final class LifecycleTest {
    @Test
    void shouldBindToPortOnStart() throws IOException {
      // given
      try (final ServerSocket socket = bindUnusedPort();
          final DebugReceiver receiver = new DebugReceiver(r -> {}, socket.getLocalPort())) {

        // when
        socket.close();
        receiver.start();

        // then
        RestAssured.given()
            .body(Collections.emptyList())
            .post(receiver.recordsEndpoint())
            .then()
            .statusCode(204);
      }
    }

    @Test
    void shouldBindToRandomPort() {
      // given
      try (final DebugReceiver receiver = new DebugReceiver(r -> {})) {
        // when
        receiver.start();

        // then
        RestAssured.given()
            .body(Collections.emptyList())
            .post(receiver.recordsEndpoint())
            .then()
            .statusCode(204);
      }
    }

    @Test
    void shouldCloseServerOnStop() {
      // given
      try (final DebugReceiver receiver = new DebugReceiver(r -> {})) {
        receiver.start();
        final URI endpoint = receiver.recordsEndpoint();

        // when
        receiver.stop();

        // then
        assertThatCode(() -> RestAssured.post(endpoint)).isInstanceOf(ConnectException.class);
      }
    }

    @Test
    void shouldDoNothingOnStartIfAlreadyStarted() {
      // given
      final DebugReceiver receiver = new DebugReceiver(r -> {});

      // when
      receiver.start();

      // then
      assertThatCode(receiver::start).doesNotThrowAnyException();
    }

    @Test
    void shouldDoNothingOnStopIfAlreadyStopped() {
      // given
      final DebugReceiver receiver = new DebugReceiver(r -> {});
      receiver.start();

      // when
      receiver.stop();

      // then
      assertThatCode(receiver::stop).doesNotThrowAnyException();
    }
  }

  @Nested
  final class ApiTest {
    @Test
    void shouldAcknowledgePosition() {
      // given
      final Record<?> record = recordFactory.generateRecord(b -> b.withPartitionId(1));
      try (final DebugReceiver receiver = new DebugReceiver(r -> {})) {
        receiver.start();

        // when
        receiver.acknowledge(1, 20L);

        // then
        //noinspection unchecked
        final Map<Object, Object> response =
            (Map<Object, Object>)
                RestAssured.given()
                    .body(Collections.singletonList(record))
                    .contentType("application/json")
                    .post(receiver.recordsEndpoint())
                    .as(Map.class);
        assertThat(response).containsEntry("position", 20);
      }
    }

    @Test
    void shouldFailWithoutBody() {
      // given
      try (final DebugReceiver receiver = new DebugReceiver(r -> {})) {
        receiver.start();

        // when - then
        RestAssured.given().post(receiver.recordsEndpoint()).then().assertThat().statusCode(400);
      }
    }

    @Test
    void shouldForwardRecordsToConsumer() {
      // given
      final List<Record<?>> consumedRecords = new CopyOnWriteArrayList<>();
      final List<Record<?>> records =
          recordFactory
              .generateRecords(b -> b.withPartitionId(1))
              .limit(10)
              .collect(Collectors.toList());
      try (final DebugReceiver receiver = new DebugReceiver(consumedRecords::add)) {
        receiver.start();

        // when
        RestAssured.given().body(records).post(receiver.recordsEndpoint());
      }

      // then
      assertThat(consumedRecords).containsExactlyElementsOf(records);
    }
  }
}

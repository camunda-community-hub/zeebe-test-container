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
package io.zeebe.containers.examples.exporter;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.MessageIntent;
import io.zeebe.containers.ZeebeContainer;
import io.zeebe.containers.exporter.DebugReceiver;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.assertj.core.groups.Tuple;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
final class BrokerWithDebugExporterTest {
  @Container private final ZeebeContainer container = new ZeebeContainer().withDebugExporter(8080);

  private final List<Record<?>> records = new CopyOnWriteArrayList<>();
  private final DebugReceiver receiver = new DebugReceiver(records::add, 8080);

  @BeforeEach
  void beforeEach() {
    receiver.start();
  }

  @AfterEach
  void afterEach() {
    receiver.close();
  }

  @Test
  void shouldReadExportedRecords() {
    // given
    try (final ZeebeClient client =
        ZeebeClient.newClientBuilder()
            .usePlaintext()
            .gatewayAddress(container.getExternalGatewayAddress())
            .build()) {

      // when
      client
          .newPublishMessageCommand()
          .messageName("myMessage")
          .correlationKey("myKey")
          .send()
          .join();
    }

    // then
    Awaitility.await("until there's at least some records exported")
        .atMost(Duration.ofSeconds(30))
        .untilAsserted(
            () ->
                assertThat(records)
                    .extracting(Record::getValueType, Record::getIntent)
                    .containsExactly(
                        Tuple.tuple(ValueType.MESSAGE, MessageIntent.PUBLISH),
                        Tuple.tuple(ValueType.MESSAGE, MessageIntent.PUBLISHED)));
  }
}

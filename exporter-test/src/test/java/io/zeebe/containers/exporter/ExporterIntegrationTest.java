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

import io.camunda.zeebe.exporter.test.ExporterTestContext;
import io.camunda.zeebe.exporter.test.ExporterTestController;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordValue;
import io.camunda.zeebe.test.broker.protocol.ProtocolFactory;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class ExporterIntegrationTest {
  private final ProtocolFactory recordFactory = new ProtocolFactory();
  private final List<Record<?>> exportedRecords = new CopyOnWriteArrayList<>();
  private final DebugReceiver receiver = new DebugReceiver(exportedRecords::add, 0, false);

  @BeforeEach
  void beforeEach() {
    receiver.start();
  }

  private static final class Partition {
    private final ExporterTestController controller = new ExporterTestController();
    private final ExporterTestContext context = new ExporterTestContext();
    private final DebugExporter exporter = new DebugExporter();

    private void prepare(final String endpoint) throws Exception {
      context.getConfiguration().getArguments().put("url", endpoint);

      exporter.configure(context);
      exporter.open(controller);
    }
  }

  @Nested
  final class SinglePartitionTest {
    private final ExporterTestController controller = new ExporterTestController();
    private final ExporterTestContext context = new ExporterTestContext();
    private final DebugExporter exporter = new DebugExporter();

    @BeforeEach
    void beforeEach() {
      context.getConfiguration().getArguments().put("url", receiver.recordsEndpoint().toString());

      exporter.configure(context);
      exporter.open(controller);
    }

    @Test
    void shouldReceiveRecords() {
      // given
      final List<Record<RecordValue>> records =
          recordFactory
              .generateRecords(b -> b.withPartitionId(1))
              .limit(10)
              .collect(Collectors.toList());

      // when
      records.forEach(exporter::export);

      // then
      assertThat(exportedRecords).containsExactlyElementsOf(records);
    }

    @Test
    void shouldAcknowledgeRecord() {
      // given
      final Record<RecordValue> record = recordFactory.generateRecord(b -> b.withPartitionId(1));
      receiver.acknowledge(1, 30L);

      // when
      exporter.export(record);

      // then
      assertThat(controller.getPosition()).isEqualTo(30L);
    }
  }

  @Nested
  final class MultiPartitionTest {
    private final Partition partitionOne = new Partition();
    private final Partition partitionTwo = new Partition();

    @BeforeEach
    void beforeEach() throws Exception {
      partitionOne.prepare(receiver.recordsEndpoint().toString());
      partitionTwo.prepare(receiver.recordsEndpoint().toString());
    }

    @Test
    void shouldReceiveRecords() {
      // given
      final Record<RecordValue> partOneRecord =
          recordFactory.generateRecord(b -> b.withPartitionId(1));
      final Record<RecordValue> partTwoRecord =
          recordFactory.generateRecord(b -> b.withPartitionId(2));

      // when
      partitionOne.exporter.export(partOneRecord);
      partitionTwo.exporter.export(partTwoRecord);

      // then
      assertThat(exportedRecords).containsExactly(partOneRecord, partTwoRecord);
    }

    @Test
    void shouldAcknowledgeRecords() {
      // given
      final Record<RecordValue> partOneRecord =
          recordFactory.generateRecord(b -> b.withPartitionId(1));
      final Record<RecordValue> partTwoRecord =
          recordFactory.generateRecord(b -> b.withPartitionId(2));
      receiver.acknowledge(1, 30L);
      receiver.acknowledge(2, 35L);

      // when
      partitionOne.exporter.export(partOneRecord);
      partitionTwo.exporter.export(partTwoRecord);

      // then
      assertThat(partitionOne.controller.getPosition()).isEqualTo(30L);
      assertThat(partitionTwo.controller.getPosition()).isEqualTo(35L);
    }
  }
}

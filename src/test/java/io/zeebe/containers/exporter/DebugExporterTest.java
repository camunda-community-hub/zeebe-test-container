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

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.http.Body;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import io.camunda.zeebe.protocol.record.Record;
import io.zeebe.containers.exporter.DebugExporter.BadRequestException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class DebugExporterTest {
  private final ProtocolFactory recordFactory = new ProtocolFactory();
  private final TestExporterApi.TestContext context = new TestExporterApi.TestContext();
  private final TestExporterApi.TestController controller = new TestExporterApi.TestController();
  private final DebugExporter exporter = new DebugExporter();

  @Test
  void shouldValidateURL() {
    // given
    context.getConfiguration().getArguments().put("url", "not a URI");

    // when - then
    assertThatCode(() -> exporter.configure(context)).isInstanceOf(MalformedURLException.class);
  }

  @Nested
  final class FaultToleranceTest {
    @Test
    void shouldNotFailOnOpenWithoutServer() throws Exception {
      // given
      context.getConfiguration().getArguments().put("url", "http://localhost:9200/records");
      exporter.configure(context);

      // when - then
      assertThatCode(() -> exporter.open(controller)).doesNotThrowAnyException();
    }

    @Test
    void shouldRetryWhenNoServer() throws Exception {
      // given
      final Record<?> record = recordFactory.generateRecord();
      context.getConfiguration().getArguments().put("url", "http://localhost:9200/records");
      exporter.configure(context);
      exporter.open(controller);

      // when - then
      assertThatCode(() -> exporter.export(record)).isInstanceOf(UncheckedIOException.class);
    }
  }

  @Nested
  @WireMockTest
  final class ServerTest {
    @BeforeEach
    void beforeEach(final WireMockRuntimeInfo serverInfo) throws Exception {
      context
          .getConfiguration()
          .getArguments()
          .put("url", serverInfo.getHttpBaseUrl() + "/records");
      exporter.configure(context);
      exporter.open(controller);
    }

    @Test
    void shouldRetryOnNonSuccessfulHttpCode() {
      // given
      final Record<?> record = recordFactory.generateRecord();
      WireMock.stubFor(WireMock.post("/records").willReturn(WireMock.aResponse().withStatus(400)));

      // when
      assertThatCode(() -> exporter.export(record)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldHandleNoResponseBody() {
      // given
      final Record<?> record = recordFactory.generateRecord();
      WireMock.stubFor(WireMock.post("/records").willReturn(WireMock.aResponse().withStatus(204)));
      controller.updateLastExportedRecordPosition(10L);

      // when
      exporter.export(record);

      // then
      assertThat(controller.position()).as("acknowledged position has not changed").isEqualTo(10L);
    }

    @Test
    void shouldHandleAcknowledgedPositionResponse() {
      // given
      final Record<?> record = recordFactory.generateRecord();
      final String body = Json.write(Collections.singletonMap("position", 20L));
      WireMock.stubFor(
          WireMock.post("/records")
              .willReturn(WireMock.aResponse().withStatus(200).withResponseBody(new Body(body))));
      controller.updateLastExportedRecordPosition(10L);

      // when
      exporter.export(record);

      // then
      assertThat(controller.position()).isEqualTo(20L);
    }

    @Test
    void shouldExportRecordAsList() {
      // given
      final Record<?> record = recordFactory.generateRecord();
      final String expectedRequestBody = Json.write(Collections.singletonList(record));
      WireMock.stubFor(WireMock.post("/records").willReturn(WireMock.aResponse().withStatus(204)));

      // when
      exporter.export(record);

      // then
      WireMock.verify(
          1,
          WireMock.postRequestedFor(WireMock.urlEqualTo("/records"))
              .withRequestBody(new EqualToJsonPattern(expectedRequestBody, true, false)));
    }

    @Test
    void shouldExportRecordsOneAtATime() {
      // given
      final Record<?> firstRecord = recordFactory.generateRecord();
      final Record<?> secondRecord = recordFactory.generateRecord();
      WireMock.stubFor(WireMock.post("/records").willReturn(WireMock.aResponse().withStatus(204)));

      // when
      exporter.export(firstRecord);
      exporter.export(secondRecord);

      // then
      WireMock.verify(
          1,
          WireMock.postRequestedFor(WireMock.urlEqualTo("/records"))
              .withRequestBody(
                  new EqualToJsonPattern(
                      Json.write(Collections.singletonList(secondRecord)), true, false)));
      WireMock.verify(
          1,
          WireMock.postRequestedFor(WireMock.urlEqualTo("/records"))
              .withRequestBody(
                  new EqualToJsonPattern(
                      Json.write(Collections.singletonList(secondRecord)), true, false)));
    }
  }
}

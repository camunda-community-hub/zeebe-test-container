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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.exporter.api.Exporter;
import io.camunda.zeebe.exporter.api.context.Context;
import io.camunda.zeebe.exporter.api.context.Controller;
import io.camunda.zeebe.protocol.record.Record;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import org.agrona.LangUtil;

/**
 * An exporter implementation which can be used for inter-process communication. It sends each
 * record as a single HTTP request. While this is not efficient, the exporter is meant to be used
 * only for non-performance testing, and is optimized for simplicity instead of performance.
 *
 * <p>The exporter class is meant to be fully standalone to simplify the exporter loading process.
 * It can still use classes available via the Zeebe broker's classloader, however.
 *
 * <p>This is an internal API - it isn't annotated to avoid having to add the dependency, as @API
 * has a runtime retention policy. It should be treated as implementation detail.
 */
public final class DebugExporter implements Exporter {
  private static final String JSON_MIME_TYPE = "application/json";
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private Context context;
  private Controller controller;
  private Config config;

  private final HttpClient client =
      HttpClient.newBuilder()
          .version(Version.HTTP_1_1)
          .followRedirects(Redirect.ALWAYS)
          .connectTimeout(Duration.ofSeconds(1))
          .executor(Runnable::run) // directly execute requests
          .build();

  @Override
  public void configure(final Context context) {
    this.context = context;
    config = Config.of(context.getConfiguration().getArguments());

    context
        .getLogger()
        .debug(
            "Configured debug IPC exporter {} with {}", context.getConfiguration().getId(), config);
  }

  @Override
  public void open(final Controller controller) {
    this.controller = controller;
    context.getLogger().info("Opened debug exporter {}", context.getConfiguration().getId());
  }

  @Override
  public void close() {
    context.getLogger().info("Closed debug IPC exporter {}", context.getConfiguration().getId());
  }

  @Override
  public void export(final Record<?> record) {
    Objects.requireNonNull(record, "must export an actual record");

    context
        .getLogger()
        .trace(
            "Exporting record {} of partition {}", record.getPosition(), record.getPartitionId());

    try {
      pushRecord(record);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      LangUtil.rethrowUnchecked(e);
    } catch (final Exception e) {
      LangUtil.rethrowUnchecked(e);
    }

    context
        .getLogger()
        .trace("Exported record {} of partition {}", record.getPosition(), record.getPartitionId());
  }

  private void pushRecord(final Record<?> record) throws IOException, InterruptedException {
    final HttpRequest request = buildRequestForRecord(record);
    final HttpResponse<byte[]> response = client.send(request, BodyHandlers.ofByteArray());
    final int statusCode = response.statusCode();
    if (statusCode >= 400) {
      throw new BadRequestException(
          String.format(
              "Failed to push out record with position %d on partition %d: response code %d",
              record.getPosition(), record.getPartitionId(), statusCode));
    }

    // is there a body to read?
    if (statusCode != 204) {
      handleAcknowledgment(response.body(), record.getPartitionId());
    }

    context
        .getLogger()
        .trace(
            "Exported record {} to {} (status code: {})", record, config.endpointURI(), statusCode);
  }

  private HttpRequest buildRequestForRecord(final Record<?> record) throws JsonProcessingException {
    return HttpRequest.newBuilder()
        .uri(config.endpointURI())
        .header("Content-Type", JSON_MIME_TYPE)
        .header("Accept", JSON_MIME_TYPE)
        .header("charset", "utf-8")
        .header("User-Agent", "ztc-debug-exporter/4.0.0")
        .timeout(Duration.ofSeconds(5))
        .POST(
            BodyPublishers.ofByteArray(MAPPER.writeValueAsBytes(Collections.singletonList(record))))
        .build();
  }

  private void handleAcknowledgment(final byte[] responseBody, final int partitionId)
      throws IOException {
    final RecordsResponse body = MAPPER.readValue(responseBody, RecordsResponse.class);
    final long position = body.position;

    if (position > -1) {
      controller.updateLastExportedRecordPosition(position);
      context
          .getLogger()
          .trace(
              "Acknowledged last exported record position {} for partition {}",
              position,
              partitionId);
    }
  }

  private record RecordsResponse(@JsonProperty("position") Long position) {}
}

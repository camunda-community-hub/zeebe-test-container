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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.exporter.api.Exporter;
import io.camunda.zeebe.exporter.api.context.Context;
import io.camunda.zeebe.exporter.api.context.Controller;
import io.camunda.zeebe.protocol.record.Record;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * An exporter implementation which can be used for inter-process communication. It sends each
 * record as a single HTTP request. While this is not efficient, the exporter is meant to be used
 * only for non-performance testing, and is optimized for simplicity instead of performance.
 *
 * <p>The exporter class is meant to be fully standalone to simplify the exporter loading process.
 * It can still use classes available via the Zeebe broker's classloader, however.
 *
 * <p>This is an internal API - it isn't annotated to avoid having to add the dependency, as @API
 * has a runtime retention policy. It should be treated as implementation detail. The actual public
 * API is using {@link io.zeebe.containers.ZeebeBrokerNode#withDebugExporter(int)} and {@link
 * DebugReceiver}.
 */
public final class DebugExporter implements Exporter {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final ConfigParser configParser;

  private Context context;
  private Controller controller;
  private Config config;

  /** Default no-args bean constructor used by the broker to load the exporter. */
  @SuppressWarnings("unused")
  public DebugExporter() {
    this(new DefaultConfigParser());
  }

  /**
   * Convenience constructor allowing some control over the configuration. Mostly used for testing.
   * Using this lets you inject or modify the configuration as you wish.
   *
   * @param configParser configuration parser
   */
  DebugExporter(final ConfigParser configParser) {
    this.configParser = configParser;
  }

  @Override
  public void configure(final Context context) throws Exception {
    this.context = context;
    config = configParser.parse(context.getConfiguration().getArguments());

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

    try (final CloseableConnection http = openConnection()) {
      pushRecord(http.connection, record);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }

    context
        .getLogger()
        .trace("Exported record {} of partition {}", record.getPosition(), record.getPartitionId());
  }

  private void pushRecord(final HttpURLConnection http, final Record<?> record) throws IOException {
    MAPPER.writeValue(http.getOutputStream(), Collections.singletonList(record));

    if (http.getResponseCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
      throw new BadRequestException(
          String.format(
              "Failed to push out record with position %d on partition %d: response code %d",
              record.getPosition(), record.getPartitionId(), http.getResponseCode()));
    }

    // is there a body to read?
    if (http.getResponseCode() != HttpURLConnection.HTTP_NO_CONTENT) {
      handleAcknowledgment(http.getInputStream(), record.getPartitionId());
    }

    context
        .getLogger()
        .trace(
            "Exported record {} to {} (status code: {})",
            record,
            config.serverUrl,
            http.getResponseCode());
  }

  private void handleAcknowledgment(final InputStream responseBody, final int partitionId)
      throws IOException {
    final ResponseDTO body = MAPPER.readValue(responseBody, ResponseDTO.class);
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

  private CloseableConnection openConnection() throws IOException {
    final HttpURLConnection connection = createHttpConnection();
    connection.connect();

    return new CloseableConnection(connection);
  }

  private HttpURLConnection createHttpConnection() throws IOException {
    final HttpURLConnection newConnection = (HttpURLConnection) config.serverUrl.openConnection();
    newConnection.setRequestMethod("POST");
    newConnection.setRequestProperty("Content-Type", "application/json");
    newConnection.setRequestProperty("charset", "utf-8");
    newConnection.setRequestProperty("User-Agent", "zpt-debug-exporter/4.0.0");
    newConnection.setConnectTimeout(2_000);
    newConnection.setReadTimeout(2_000);
    newConnection.setInstanceFollowRedirects(true);
    newConnection.setUseCaches(true);
    newConnection.setDoOutput(true);
    newConnection.setDoInput(true);
    return newConnection;
  }

  static final class BadRequestException extends RuntimeException {

    private BadRequestException(final String message) {
      super(message);
    }
  }

  private static final class Config {
    private final URL serverUrl;

    public Config(final URL serverUrl) {
      this.serverUrl = serverUrl;
    }
  }

  private static final class DefaultConfigParser implements ConfigParser {
    @Override
    public Config parse(final Map<String, Object> args) throws MalformedURLException {
      final String rawUrl = (String) args.getOrDefault("url", "");
      return new Config(new URL(rawUrl));
    }
  }

  @SuppressWarnings("FieldMayBeFinal")
  private static final class ResponseDTO {
    @JsonProperty(value = "position")
    private Long position = -1L;
  }

  private static final class CloseableConnection implements AutoCloseable {
    private final HttpURLConnection connection;

    private CloseableConnection(final HttpURLConnection connection) {
      this.connection = Objects.requireNonNull(connection);
    }

    @Override
    public void close() {
      connection.disconnect();
    }
  }

  @FunctionalInterface
  interface ConfigParser {
    Config parse(final Map<String, Object> rawConfig) throws IOException;
  }
}

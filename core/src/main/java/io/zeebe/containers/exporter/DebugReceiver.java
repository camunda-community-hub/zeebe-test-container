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

import io.camunda.zeebe.protocol.record.Record;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.hc.core5.http.URIScheme;
import org.apache.hc.core5.http.config.CharCodingConfig;
import org.apache.hc.core5.http.impl.HttpProcessors;
import org.apache.hc.core5.http.impl.bootstrap.AsyncServerBootstrap;
import org.apache.hc.core5.http.impl.bootstrap.HttpAsyncServer;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.reactor.ListenerEndpoint;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Receives records sent from one or more debug exporter instances. The receiver will start an HTTP
 * server at the given port, exposing a single route: POST /records. This endpoint expects a list of
 * {@link Record} objects, serialized to JSON.
 *
 * <p>See {@link RecordHandler} for documentation about the /records endpoint.
 */
@API(status = Status.EXPERIMENTAL)
public final class DebugReceiver implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(DebugReceiver.class);

  private final RecordHandler recordHandler;
  private final HttpAsyncServer server;
  private final SocketAddress unboundAddress;

  private boolean started;
  private ListenerEndpoint endpoint;

  /**
   * A debug receiver which binds to localhost using a random available port on start, forwarding
   * all records to the given consumer.
   *
   * @param recordConsumer a consumer called every time a record is received
   * @throws NullPointerException if any {@code recordConsumer} is null
   */
  public DebugReceiver(final Consumer<Record<?>> recordConsumer) {
    this(recordConsumer, 0);
  }

  /**
   * A debug receiver which binds to localhost using the given port on start, forwarding all records
   * * to the given consumer.
   *
   * @param recordConsumer a consumer called every time a record is received
   * @param port the port to bind to; can be 0 to grab a random port
   * @throws NullPointerException if any {@code recordConsumer} is null
   */
  public DebugReceiver(final Consumer<Record<?>> recordConsumer, final int port) {
    this(recordConsumer, new InetSocketAddress("localhost", port));
  }

  /**
   * A debug receiver which binds to {@code address} on start, and forwards all record to the {@code
   * recordConsumer}.
   *
   * @param recordConsumer the consumer which will receive records
   * @param address the address to bind to on start
   * @throws NullPointerException if any of the arguments are null
   */
  public DebugReceiver(final Consumer<Record<?>> recordConsumer, final InetSocketAddress address) {
    this(new RecordHandler(recordConsumer), address);
  }

  /**
   * Convenience constructor used, primarily useful for testing.
   *
   * @param recordHandler the record handler to use
   * @param unboundAddress the address that the server will bind to
   * @throws NullPointerException if any of the arguments are null
   */
  DebugReceiver(final RecordHandler recordHandler, final InetSocketAddress unboundAddress) {
    this.unboundAddress = Objects.requireNonNull(unboundAddress, "must specify a bind address");
    this.recordHandler = Objects.requireNonNull(recordHandler, "must specify a record handler");

    server = createServer();
  }

  /**
   * Updates the acknowledged position up to position on the given partition. Note that this will
   * not take effect immediately, but instead will only be applied when answering the next export
   * request for this partition.
   *
   * @param partitionId the partition ID on which to acknowledge
   * @param position the position of the record to acknowledge
   */
  public void acknowledge(final int partitionId, final long position) {
    recordHandler.acknowledge(partitionId, position);
  }

  /**
   * Returns the server's bind address (if any). Will fail if the server is not bound, i.e. the
   * receiver was not started.
   *
   * <p>NOTE: you can pass 0 as the port when constructing your receiver, and grab the bind address
   * afterwards using this method.
   *
   * @return the server's bind address
   * @throws IllegalStateException if the server was not yet started
   */
  public synchronized SocketAddress serverAddress() {
    if (!started || endpoint == null) {
      throw new IllegalStateException(
          "Cannot get server bind address until the receiver is opened");
    }

    return endpoint.getAddress();
  }

  /**
   * Returns the URI of the `/records` endpoint, i.e. where records should be exported.
   *
   * @return the URI of the `/records` endpoint
   * @throws IllegalStateException if the server was not yet started
   */
  public synchronized URI recordsEndpoint() {
    if (!started || endpoint == null) {
      throw new IllegalStateException("Cannot get records endpoint until the receiver is opened");
    }

    // string conversion of an InetSocketAddress always adds `/` in front of the host, so we only
    // need a single / here after the scheme
    return URI.create(String.format("http:/%s/records", endpoint.getAddress()));
  }

  /**
   * Opens the receiver, mapping the export and ack files into memory. The receiver cannot consume
   * nor acknowledge without being opened.
   */
  public void start() {
    if (started) {
      return;
    }

    try {
      server.start();
      endpoint = server.listen(unboundAddress, URIScheme.HTTP).get();
      started = true;
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      LOGGER.warn(
          "Interrupted while awaiting server bind; if in the process of shutting down, can be "
              + "ignored",
          e);
    } catch (final Exception e) {
      LOGGER.warn("Failed to open debug receiver", e);
      close();
    }
  }

  /**
   * Stops the receiver, freeing any resources allocated in {@link #start()}. It is not possible to
   * consume nor acknowledge records once closed.
   */
  public void stop() {
    if (!started) {
      return;
    }

    server.close(CloseMode.IMMEDIATE);
    started = false;
  }

  /**
   * Closes the receiver, freeing any resources allocated in {@link #start()}. It is not possible to
   * consume nor acknowledge records once closed.
   */
  @Override
  public void close() {
    stop();
  }

  private HttpAsyncServer createServer() {
    final IOReactorConfig config =
        IOReactorConfig.custom()
            .setIoThreadCount(2)
            .setSoReuseAddress(true)
            .setSoTimeout(5, TimeUnit.SECONDS)
            .setTcpNoDelay(true)
            .build();

    return AsyncServerBootstrap.bootstrap()
        .setIOReactorConfig(config)
        .setCanonicalHostName("localhost")
        .setCharCodingConfig(CharCodingConfig.custom().setCharset(StandardCharsets.UTF_8).build())
        .setHttpProcessor(HttpProcessors.server("zpt-debug/1.1"))
        .register("/records", recordHandler)
        .create();
  }
}

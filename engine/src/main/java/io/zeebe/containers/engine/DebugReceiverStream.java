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

import io.camunda.zeebe.process.test.api.RecordStreamSource;
import io.camunda.zeebe.protocol.record.Record;
import io.zeebe.containers.ZeebeBrokerNode;
import io.zeebe.containers.exporter.DebugReceiver;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import net.jcip.annotations.ThreadSafe;
import org.agrona.collections.MutableInteger;
import org.agrona.collections.MutableLong;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/**
 * Implementation of {@link RecordStreamSource} which wraps an infinite list. It will manage an
 * instance of a {@link DebugReceiver} which will add records to the infinite list, and use that
 * list as the record stream.
 *
 * <p>In order to wait for idle and busy states, we currently use two hacks:
 *
 * <ul>
 *   <li>idle: idle means no records were exported for 1 second
 *   <li>busy: busy means a new record is exported after the call
 * </ul>
 */
@API(status = Status.INTERNAL)
@ThreadSafe
final class DebugReceiverStream implements RecordStreamSource, AutoCloseable {
  private final InfiniteList<Record<?>> records;
  private final DebugReceiver receiver;
  private final Duration idlePeriod;

  DebugReceiverStream(final InfiniteList<Record<?>> records) {
    this(records, new DebugReceiver(records::add));
  }

  DebugReceiverStream(final InfiniteList<Record<?>> records, final Duration idlePeriod) {
    this(records, new DebugReceiver(records::add), idlePeriod);
  }

  DebugReceiverStream(final InfiniteList<Record<?>> records, final DebugReceiver receiver) {
    this(records, receiver, Duration.ofSeconds(1));
  }

  DebugReceiverStream(
      final InfiniteList<Record<?>> records,
      final DebugReceiver receiver,
      final Duration idlePeriod) {
    this.records = records;
    this.receiver = receiver;
    this.idlePeriod = idlePeriod;
  }

  void start(final Collection<? extends ZeebeBrokerNode<?>> brokers) {
    receiver.start();

    final int port = receiver.serverAddress().getPort();
    brokers.forEach(broker -> broker.withDebugExporter(port));
  }

  void stop() {
    receiver.stop();
  }

  void acknowledge(final int partitionId, final long position) {
    receiver.acknowledge(partitionId, position);
  }

  @Override
  public void close() {
    stop();
  }

  @Override
  public Iterable<Record<?>> getRecords() {
    return records;
  }

  void waitForIdleState(final Duration timeout) throws InterruptedException, TimeoutException {
    final MutableInteger recordsCount = new MutableInteger(records.size());
    final MutableLong lastInvoked = new MutableLong(System.nanoTime());
    final MutableLong conditionHeld = new MutableLong(0L);
    final Duration pollingInterval = Duration.ofMillis(100);
    final long mustHold = idlePeriod.toNanos();

    awaitConditionHolds(
        timeout,
        pollingInterval,
        "until no records are exported for 1 second",
        () -> {
          final int count = recordsCount.get();
          recordsCount.set(records.size());

          final long currentNano = System.nanoTime();
          final long timeElapsed = currentNano - lastInvoked.get();
          lastInvoked.set(currentNano);

          if (count == recordsCount.get()) {
            conditionHeld.set(conditionHeld.get() + timeElapsed);
          } else {
            conditionHeld.set(0L);
          }

          return conditionHeld.get() >= mustHold;
        });
  }

  void waitForBusyState(final Duration timeout) throws InterruptedException, TimeoutException {
    final MutableInteger recordsCount = new MutableInteger(records.size());
    awaitConditionHolds(
        timeout,
        Duration.ofMillis(100),
        "until a record is exported",
        () -> {
          final int count = recordsCount.get();
          recordsCount.set(records.size());
          return count != recordsCount.get();
        });
  }

  private void awaitConditionHolds(
      final Duration timeout,
      final Duration pollInterval,
      final String description,
      final BooleanSupplier condition)
      throws TimeoutException, InterruptedException {
    final Thread current = Thread.currentThread();

    final long timeoutNs = System.nanoTime() + timeout.toNanos();
    while (!current.isInterrupted() && System.nanoTime() < timeoutNs) {
      if (condition.getAsBoolean()) {
        break;
      }

      // we do want to busy wait here
      //noinspection BusyWait
      Thread.sleep(pollInterval.toMillis());
    }

    final boolean timedOut = System.nanoTime() >= timeoutNs;
    if (timedOut) {
      throw new TimeoutException("Timed out waiting " + description);
    }
  }
}

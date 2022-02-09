package io.zeebe.containers.test;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.process.test.api.InMemoryEngine;
import io.camunda.zeebe.process.test.api.RecordStreamSource;
import io.zeebe.containers.ZeebeContainer;
import io.zeebe.containers.clock.ZeebeClock;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

@API(status = Status.EXPERIMENTAL)
public final class ZeebeContainerEngine implements InMemoryEngine {
  private final ZeebeContainer container;
  private final ZeebeClock clock;
  private final DebugExporterConsumer consumer;
  private final ConsumerRecordStreamSource recordStreamSource;

  public ZeebeContainerEngine(final ZeebeContainer container, final int debugHttpPort) {
    this.container = container;
    clock = ZeebeClock.newDefaultClock(container);

    final Lock lock = new ReentrantLock();
    final Condition isEmptyCondition = lock.newCondition();
    consumer =
        new DebugExporterConsumer(
            Collections.singleton(new DebugExporterTarget(container, debugHttpPort)),
            lock,
            isEmptyCondition);
    recordStreamSource = new ConsumerRecordStreamSource(consumer, lock, isEmptyCondition);
  }

  @Override
  public void start() {
    consumer.start();
  }

  @Override
  public void stop() {
    consumer.stop();
  }

  @Override
  public RecordStreamSource getRecordStream() {
    return recordStreamSource;
  }

  @Override
  public ZeebeClient createClient() {
    return ZeebeClient.newClientBuilder()
        .usePlaintext()
        .gatewayAddress(container.getExternalGatewayAddress())
        .build();
  }

  @Override
  public String getGatewayAddress() {
    return container.getExternalGatewayAddress();
  }

  @Override
  public void increaseTime(final Duration duration) {
    clock.addTime(duration);
  }

  @Override
  public void runOnIdleState(final Runnable runnable) {
    throw new UnsupportedOperationException(
        "runOnIdleState is not currently supported against a full Zeebe broker");
  }

  @Override
  public void waitForIdleState() {
    throw new UnsupportedOperationException(
        "waitForIdleState is not currently supported against a full Zeebe broker");
  }
}

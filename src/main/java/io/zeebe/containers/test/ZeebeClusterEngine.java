package io.zeebe.containers.test;

import feign.Target;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.process.test.api.InMemoryEngine;
import io.camunda.zeebe.process.test.api.RecordStreamSource;
import io.zeebe.containers.ZeebeGatewayNode;
import io.zeebe.containers.clock.ZeebeClock;
import io.zeebe.containers.cluster.ZeebeCluster;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.agrona.CloseHelper;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

@API(status = Status.EXPERIMENTAL)
public final class ZeebeClusterEngine implements InMemoryEngine {
  private final ZeebeCluster cluster;
  private final Collection<ZeebeClock> clocks;
  private final DebugExporterConsumer consumer;
  private final ConsumerRecordStreamSource recordStreamSource;

  public ZeebeClusterEngine(final ZeebeCluster cluster, final int debugHttpPort) {
    this.cluster = cluster;
    clocks = new ArrayList<>();
    cluster.getNodes().forEach((ignored, node) -> clocks.add(ZeebeClock.newDefaultClock(node)));

    final Lock lock = new ReentrantLock();
    final Condition isEmptyCondition = lock.newCondition();
    final List<Target<DebugExporterClient>> consumerTargets =
        cluster.getBrokers().values().stream()
            .map(broker -> new DebugExporterTarget(broker, debugHttpPort))
            .collect(Collectors.toList());
    consumer = new DebugExporterConsumer(consumerTargets, lock, isEmptyCondition);
    recordStreamSource = new ConsumerRecordStreamSource(consumer, lock, isEmptyCondition);
  }

  @Override
  public void start() {
    cluster.start();
    consumer.start();
  }

  @Override
  public void stop() {
    CloseHelper.quietCloseAll(consumer, cluster);
  }

  @Override
  public RecordStreamSource getRecordStream() {
    return recordStreamSource;
  }

  @Override
  public ZeebeClient createClient() {
    return cluster.newClientBuilder().build();
  }

  @Override
  public String getGatewayAddress() {
    return getRandomGateway().getExternalGatewayAddress();
  }

  @Override
  public void increaseTime(final Duration duration) {
    clocks.forEach(clock -> clock.addTime(duration));
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

  private ZeebeGatewayNode<?> getRandomGateway() {
    final int gatewaysCount = cluster.getGateways().size();
    if (gatewaysCount == 0) {
      throw new UnsupportedOperationException("No gateways were configured for this cluster");
    }

    final int gatewayIndex = ThreadLocalRandom.current().nextInt(0, gatewaysCount - 1);
    return cluster.getGateways().values().stream()
        .skip(gatewayIndex)
        .findFirst()
        .orElseThrow(
            () ->
                new UnsupportedOperationException(
                    "Unexpected error; there should have been one gateway to return"));
  }
}

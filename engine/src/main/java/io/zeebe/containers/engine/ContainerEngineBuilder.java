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

import io.camunda.zeebe.protocol.record.Record;
import io.zeebe.containers.ZeebeBrokerNode;
import io.zeebe.containers.ZeebeContainer;
import io.zeebe.containers.ZeebeGatewayNode;
import io.zeebe.containers.cluster.ZeebeCluster;
import io.zeebe.containers.engine.ContainerEngine.Builder;
import io.zeebe.containers.exporter.DebugReceiver;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

@API(status = Status.INTERNAL)
final class ContainerEngineBuilder implements Builder {
  private static final Duration DEFAULT_IDLE_PERIOD = Duration.ofSeconds(1);
  private static final Duration DEFAULT_GRACE_PERIOD = Duration.ZERO;
  private static final Logger LOGGER = LoggerFactory.getLogger(ContainerEngineBuilder.class);
  private static final Duration MINIMUM_IDLE_PERIOD = Duration.ofMillis(100);
  private Holder<? extends GenericContainer<?>> container;
  private ZeebeCluster cluster;
  private Duration idlePeriod;
  private Duration gracePeriod;
  private boolean autoAcknowledge;

  @Override
  public <T extends GenericContainer<T> & ZeebeGatewayNode<T> & ZeebeBrokerNode<T>>
      ContainerEngineBuilder withContainer(final T container) {
    if (cluster != null) {
      LOGGER.warn("Setting a container will overwrite the previously assigned cluster");
      cluster = null;
    }

    this.container = new Holder<>(Objects.requireNonNull(container, "must specify a container"));
    return this;
  }

  @Override
  public ContainerEngineBuilder withCluster(final ZeebeCluster cluster) {
    if (container != null) {
      LOGGER.warn("Setting a cluster will overwrite the previously assigned container");
      container = null;
    }

    this.cluster = Objects.requireNonNull(cluster, "must specify a cluster");
    return this;
  }

  @Override
  public ContainerEngineBuilder withIdlePeriod(final Duration idlePeriod) {
    Objects.requireNonNull(idlePeriod, "must specify an idle period");
    if (idlePeriod.compareTo(MINIMUM_IDLE_PERIOD) < 0) {
      throw new IllegalArgumentException(
          String.format(
              "Cannot assign idle period [%s] less than the minimum [%s]",
              idlePeriod, MINIMUM_IDLE_PERIOD));
    }

    this.idlePeriod = idlePeriod;
    return this;
  }

  @Override
  public ContainerEngineBuilder withGracePeriod(final Duration gracePeriod) {
    Objects.requireNonNull(gracePeriod, "must specify an grace period");
    if (gracePeriod.isNegative()) {
      LOGGER.warn("Cannot assign negative grace period {}; will default to 0", gracePeriod);
      this.gracePeriod = DEFAULT_GRACE_PERIOD;
    } else {
      this.gracePeriod = gracePeriod;
    }

    return this;
  }

  @Override
  public Builder withAutoAcknowledge(final boolean autoAcknowledge) {
    this.autoAcknowledge = autoAcknowledge;
    return this;
  }

  @SuppressWarnings("unchecked")
  public ContainerEngine build() {
    final Duration listGracePeriod = Optional.ofNullable(gracePeriod).orElse(DEFAULT_GRACE_PERIOD);
    final Duration receiveIdlePeriod = Optional.ofNullable(idlePeriod).orElse(DEFAULT_IDLE_PERIOD);
    final InfiniteList<Record<?>> records = new InfiniteList<>(listGracePeriod);
    final DebugReceiverStream recordStream =
        new DebugReceiverStream(
            records, new DebugReceiver(records::add, autoAcknowledge), receiveIdlePeriod);

    try {
      if (container != null) {
        return new ZeebeContainerEngine(container.container, recordStream);
      }

      if (cluster != null) {
        return new ZeebeClusterEngine(cluster, recordStream);
      }

      return new ZeebeContainerEngine<>(new ZeebeContainer(), recordStream);
    } catch (final Exception e) {
      recordStream.close();
      throw e;
    }
  }

  private static final class Holder<
      T extends GenericContainer<T> & ZeebeGatewayNode<T> & ZeebeBrokerNode<T>> {
    private final T container;

    private Holder(final T container) {
      this.container = container;
    }
  }
}

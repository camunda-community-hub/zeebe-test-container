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

import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.protocol.record.Record;
import io.zeebe.containers.ZeebeContainer;
import io.zeebe.containers.cluster.ZeebeCluster;
import java.time.Duration;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/**
 * Collection of factories which create {@link ContainerEngine} instances based on either containers
 * ({@link ZeebeContainer}) or clusters ({@link ZeebeCluster}).
 *
 * <p>In each case, you can configure a grace period for which to wait for events. This may help you
 * deal with asynchronicity in your tests in a more succinct way. For example, instead of wrapping
 * an assertion in an `Awaitility` block, you could configure a grace period of 10 seconds and use
 * {@link io.camunda.zeebe.process.test.assertions.BpmnAssert} as you normally would. When the
 * assertion reaches the end of its stream, it will wait up to the grace period for new events
 * before giving up. This means, in our example, it would wait up to 10 seconds for new events. Once
 * a new event comes in during the grace period, that period is reset.
 *
 * <p>There are some differences with the normal {@link ZeebeTestEngine} implementations from the
 * zeebe-process-test project, notably in how we wait for idle/busy states. As there are no external
 * ways to do so with a real Zeebe engine, we define these states as:
 *
 * <ul>
 *   <li>idle: no records were exported for at least 1 second since the call
 *   <li>busy: a new record was exported within the timeout since the call
 * </ul>
 */
@SuppressWarnings("unused")
@API(status = Status.EXPERIMENTAL)
public final class ContainerEngines {
  private ContainerEngines() {}

  /**
   * Returns a container engine pointing to a default {@link ZeebeContainer}, with a non-delayed
   * record stream.
   *
   * <p>If you wish to customize the container, consider declaring it separately and using {@link
   * #of(ZeebeContainer)}.
   *
   * @return a single-container engine
   */
  public static ContainerEngine of() {
    return of(Duration.ZERO);
  }

  /**
   * Returns a container engine pointing to a default {@link ZeebeContainer}. When the underlying
   * record stream reaches the end, it will wait up to at most {@code timeout} duration for new
   * events. If a new event comes in within that grace period, the timeout is reset.
   *
   * <p>You can use this to succinctly deal with asynchronicity in your tests, instead of wrapping
   * with Awaitility.
   *
   * <p>If you wish to customize the container, consider declaring it separately and using {@link
   * #of(Duration, ZeebeContainer)}.
   *
   * @param timeout the grace period to wait for new exported events
   * @return a single-container engine with a grace period
   */
  public static ContainerEngine of(final Duration timeout) {
    return of(timeout, new ZeebeContainer());
  }

  /**
   * Returns a container engine pointing to the provided {@link ZeebeContainer}, with a non-delayed
   * record stream.
   *
   * @return a single-container engine
   */
  public static ContainerEngine of(final ZeebeContainer container) {
    return of(Duration.ZERO, container);
  }

  /**
   * Returns a container engine pointing to a configured {@link ZeebeContainer}. When the underlying
   * record stream reaches the end, it will wait up to at most {@code timeout} duration for new
   * events. If a new event comes in within that grace period, the timeout is reset.
   *
   * <p>You can use this to succinctly deal with asynchronicity in your tests, instead of wrapping
   * with Awaitility.
   *
   * @param timeout the grace period to wait for new exported events
   * @return a single-container engine with a grace period
   */
  @SuppressWarnings("java:S2095")
  public static ContainerEngine of(final Duration timeout, final ZeebeContainer container) {
    final InfiniteList<Record<?>> records = new InfiniteList<>(timeout);
    return new ZeebeContainerEngine<>(container, new DebugReceiverStream(records));
  }

  /**
   * Returns a container engine pointing to a configured {@link ZeebeCluster}, with a non-delayed
   * record stream.
   *
   * <p>Records will be streamed from all partition leaders at the same time, so you can run
   * assertions across your whole cluster easily.
   *
   * <p>When manipulating the clock through the {@link ZeebeTestEngine} interface, it will
   * manipulate the clock of all brokers at the same time.
   *
   * <p>When obtaining a gateway, it will return a random available healthy gateway.
   *
   * @return a single-container engine
   */
  public static ContainerEngine of(final ZeebeCluster cluster) {
    return of(Duration.ZERO, cluster);
  }

  /**
   * Returns a container engine pointing to a configured {@link ZeebeCluster}. When the underlying
   * record stream reaches the end, it will wait up to at most {@code timeout} duration for new
   * events. If a new event comes in within that grace period, the timeout is reset.
   *
   * <p>You can use this to succinctly deal with asynchronicity in your tests, instead of wrapping
   * with Awaitility.
   *
   * <p>Records will be streamed from all partition leaders at the same time, so you can run
   * assertions across your whole cluster easily.
   *
   * <p>When manipulating the clock through the {@link ZeebeTestEngine} interface, it will
   * manipulate the clock of all brokers at the same time.
   *
   * <p>When obtaining a gateway, it will return a random available healthy gateway.
   *
   * @param timeout the grace period to wait for new exported events
   * @return a single-container engine with a grace period
   */
  @SuppressWarnings("java:S2095")
  public static ContainerEngine of(final Duration timeout, final ZeebeCluster cluster) {
    final InfiniteList<Record<?>> records = new InfiniteList<>(timeout);
    return new ZeebeClusterEngine(cluster, new DebugReceiverStream(records));
  }
}

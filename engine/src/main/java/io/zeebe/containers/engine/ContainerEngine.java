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
import io.camunda.zeebe.process.test.assertions.ProcessInstanceAssert;
import io.zeebe.containers.ZeebeBrokerNode;
import io.zeebe.containers.ZeebeGatewayNode;
import io.zeebe.containers.cluster.ZeebeCluster;
import io.zeebe.containers.exporter.DebugReceiver;
import java.time.Duration;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startable;

/**
 * A {@link ContainerEngine} is a {@link ZeebeTestEngine} implementation which wraps a container or
 * a set of containers.
 *
 * <p>You can use the provided {@link Builder} interface (via {@link #builder()} to build a custom
 * engine.
 */
@API(status = Status.EXPERIMENTAL)
public interface ContainerEngine extends Startable, ZeebeTestEngine {

  /**
   * Returns a default builder. Calling {@link Builder#build()} on a fresh builder will return a
   * builder wrapping a default {@link io.zeebe.containers.ZeebeContainer}, with an idle period of 1
   * second.
   *
   * @return a new {@link Builder} instance
   */
  static Builder builder() {
    return new ContainerEngineBuilder();
  }

  /**
   * Creates a {@link ContainerEngine} with a default {@link io.zeebe.containers.ZeebeContainer}, an
   * idle period of 1 second, a grace period of 0, and auto acknowledging all records.
   *
   * @return a new, default container engine
   */
  static ContainerEngine createDefault() {
    return builder().build();
  }

  /**
   * Marks all records with a position less than {@code position} on partition with ID {@code
   * partitionId} as acknowledged, meaning they can now be deleted from Zeebe.
   *
   * <p>Note that this is not a synchronous operation, but instead will take effect when the next
   * record is exported. See {@link io.zeebe.containers.exporter.DebugReceiver#acknowledge(int,
   * long)} for more.
   *
   * @param partitionId the ID of the partition on which to acknowledge
   * @param position the position up to which they should be acknowledged
   */
  void acknowledge(final int partitionId, final long position);

  /**
   * A helper class to build {@link ContainerEngine} instances. A fresh, non-configured builder will
   * always return one which has an idle period of 1 second, and uses a default {@link
   * io.zeebe.containers.ZeebeContainer} as its gateway and broker.
   *
   * <p>The builder can wrap either a {@link GenericContainer} which implements {@link
   * ZeebeGatewayNode} and {@link ZeebeBrokerNode}, or a {@link ZeebeCluster}, but not both. Setting
   * one will overwrite and nullify any previous assignment to either.
   *
   * <p>The default idle period is 1 second, and the default grace period is 0.
   */
  interface Builder {

    /**
     * Sets the given container to be used as both gateway and broker. Will set any assigned cluster
     * (via {@link #withCluster(ZeebeCluster)} to null.
     *
     * @param container the container to use as a gateway and broker
     * @return itself for chaining
     * @param <T> the concrete type of the container, e.g. {@link
     *     io.zeebe.containers.ZeebeContainer}
     */
    <T extends GenericContainer<T> & ZeebeGatewayNode<T> & ZeebeBrokerNode<T>>
        Builder withContainer(final T container);

    /**
     * Sets the given cluster to be used as engine(s)/gateway(s).
     *
     * <p>When using a cluster, calls to {@link ZeebeTestEngine#increaseTime(Duration)} will
     * increase the time on all nodes of the cluster. Additionally, calls to {@link
     * ZeebeTestEngine#getGatewayAddress()} will return the address of a random, available gateway,
     * and thus may not always return the same value if there are multiple gateways. Finally, calls
     * to {@link ZeebeTestEngine#createClient()} will create a client pointing to a random available
     * gateway. If that gateway shuts down (gracefully or not), the client will not know how to
     * reconnect, and a new client must be obtained.
     *
     * @param cluster the cluster to wrap
     * @return itself for chaining
     */
    Builder withCluster(final ZeebeCluster cluster);

    /**
     * Sets the idle period of the engine, used when calling {@link
     * ZeebeTestEngine#waitForIdleState(Duration)}. In a {@link ContainerEngine}, we define the
     * engine to be idle if no records have been exported during the idle period. While this is not
     * extremely accurate (there could be some issue with the engine, after all), it's the best we
     * can do from the outside.
     *
     * <p>By default, the idle period is 1 second.
     *
     * @param idlePeriod how long no records must have been exported for the engine to be considered
     *     idle
     * @return itself for chaining
     */
    Builder withIdlePeriod(final Duration idlePeriod);

    /**
     * Sets the grace period to use when reaching the end of the underlying {@link
     * io.camunda.zeebe.process.test.api.RecordStreamSource}.
     *
     * <p>When a positive grace period is configured, upon reaching the end of the stream, any
     * assertion will wait for new records until the grace period is expired. If a record is
     * appended to the stream during the grace period, the period is reset.
     *
     * <p>What this means concretely is, if you call something like {@link
     * ProcessInstanceAssert#isCompleted()} immediately after starting a process instance, it will
     * block and wait up to the grace period for new records to be processed/emitted before
     * returning.
     *
     * <p>While not required, setting this can be a useful alternative to calling {@link
     * ZeebeTestEngine#waitForIdleState(Duration)}.
     *
     * <p>NOTE: one of the pitfalls with this method however is that certain assertions, notably
     * those7 which check for the <em>absence</em> of something, will typically block for the
     * complete duration * of the grace period, thus slowing down your tests.
     *
     * @param gracePeriod the grace period to use when reaching the end of the record stream
     * @return itself for chaining
     */
    Builder withGracePeriod(final Duration gracePeriod);

    /**
     * Sets whether records should be automatically acknowledged as they are exported by the broker.
     * If true, then as soon as a record is received, it will be eligible for deletion in Zeebe. If
     * false, then records must be explicitly acknowledged by the user via {@link #acknowledge(int,
     * long)}.
     *
     * <p>By default, this is true.
     *
     * @param acknowledge whether to automatically acknowledge exported records or not
     * @return itself for chaining
     * @deprecated since 3.5.2, will be removed in 3.7.0; use {@link #withDebugReceiver(DebugReceiver)} instead
     */
    @Deprecated
    Builder withAutoAcknowledge(final boolean acknowledge);

    /**
     * Pre-assigns the port to use for the debug receive, i.e. the socket used to receive exported
     * records from the container(s). By default, this is 0, i.e. a random port.
     *
     * @param port the port to assign to the receiver
     * @return itself for chaining
     * @deprecated since 3.5.2, will be removed in 3.7.0; use {@link
     *     #withDebugReceiver(DebugReceiver)} instead
     */
    @Deprecated
    Builder withDebugReceiverPort(final int port);

    /**
     * The pre-configured {@link DebugReceiver} instance to use. Useful if you want to pre-assign
     * ports or have fine-grained control over the acknowledgment process.
     *
     * @param receiver the debug receiver to use
     * @return itself for chaining
     */
    Builder withDebugReceiver(final DebugReceiver receiver);

    /**
     * Builds a {@link ContainerEngine} based on the configuration. If nothing else was called, will
     * build an engine using a default {@link io.zeebe.containers.ZeebeContainer}, an idle period of
     * 1 second, and a grace period of 0.
     *
     * @return a new, stopped container engine
     */
    ContainerEngine build();
  }
}

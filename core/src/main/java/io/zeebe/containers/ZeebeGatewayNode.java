/*
 * Copyright © 2019 camunda services GmbH (info@camunda.com)
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
package io.zeebe.containers;

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.testcontainers.containers.GenericContainer;

/**
 * Represents common properties of nodes which can act as a gateway for a Zeebe cluster, e.g. {@link
 * ZeebeContainer} or {@link ZeebeGatewayContainer}.
 *
 * <p>You can use {@link #getExternalGatewayAddress()} for clients which are not part of the gateway
 * container's network; this is most likely what you want to use, so when in doubt use this.
 *
 * <p>You can use {@link #getInternalGatewayAddress()} for clients which are within the gateway
 * container's network.
 *
 * @param <T> the concrete type of the underlying container
 */
@API(status = Status.STABLE)
@SuppressWarnings({"UnusedReturnValue", "unused"})
public interface ZeebeGatewayNode<T extends GenericContainer<T> & ZeebeGatewayNode<T>>
    extends ZeebeNode<T> {

  /**
   * Override the default topology check to change the number of expected partitions or the gateway
   * port if necessary. Overwrites previously set wait strategies with the container's default wait
   * strategy and the given topology check.
   *
   * <p>For example, to change the number of expected partitions for a complete topology, you can
   * do:
   *
   * <pre>{@code
   * gateway.withTopologyCheck(new TopologyWaitStrategy().forExpectedPartitionsCount(3));
   * }</pre>
   *
   * <p>NOTE: this may mutate the underlying wait strategy, so if you are configured a specific
   * startup timeout, it will need to be applied again after a call to this method.
   *
   * @param topologyCheck the new topology check
   * @return this container, for chaining
   */
  T withTopologyCheck(final ZeebeTopologyWaitStrategy topologyCheck);

  /**
   * Convenience method to disable the topology check. Overwrites previously set wait strategies
   * with the container's default wait strategy, without the topology check.
   *
   * @return this container, for chaining
   */
  T withoutTopologyCheck();

  /**
   * Returns address a client which is not part of the container's network should use. If you're
   * unsure whether you need to use the external or internal address, then you most likely want the
   * external address.
   *
   * <p>You can build your client like this:
   *
   * <pre>@{code
   *   ZeebeClient.newClientBuilder()
   *     .withBrokerContactPoint(container.getExternalGatewayAddress())
   *     .usePlaintext()
   *     .build();
   * }</pre>
   *
   * @return the gateway address visible from outside the docker network
   */
  default String getExternalGatewayAddress() {
    return getExternalAddress(ZeebePort.GATEWAY.getPort());
  }

  /**
   * Returns an address a client which is part of the container's network can use.
   *
   * <p>You can build your client like this:
   *
   * <pre>@{code
   *   ZeebeClient.newClientBuilder()
   *     .withBrokerContactPoint(container.getInternalGatewayAddress())
   *     .usePlaintext()
   *     .build();
   * }</pre>
   *
   * @return the gateway address visible from within the docker network
   */
  default String getInternalGatewayAddress() {
    return getInternalAddress(ZeebePort.GATEWAY.getPort());
  }
}

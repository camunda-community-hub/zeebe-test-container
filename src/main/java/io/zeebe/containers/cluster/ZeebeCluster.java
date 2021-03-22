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
package io.zeebe.containers.cluster;

import com.google.common.collect.Streams;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.ZeebeClientBuilder;
import io.zeebe.containers.ZeebeBrokerNode;
import io.zeebe.containers.ZeebeGatewayNode;
import io.zeebe.containers.ZeebeNode;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.lifecycle.Startables;

/**
 * A convenience class representing a one or more containers that form a Zeebe cluster.
 *
 * <p>It's recommended to use the {@link ZeebeClientBuilder} to build one.
 *
 * <p>As the cluster is not started automatically, the containers can still be modified/configured
 * beforehand. Be aware however that the replication factor and the partitions count cannot be
 * modified: if you configure different values directly on your brokers, then you may run into
 * issues. Keep in mind as well that the gateways and brokers should be treated as immutable
 * collections.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 *   final class MyClusteredTest {
 *     private ZeebeCluster cluster;
 *     private Network network;
 *
 *     &#64;BeforeEach
 *     void beforeEach() {
 *       network = Network.newNetwork();
 *       cluster = ZeebeCluster.builder()
 *           .withBrokersCount(3)
 *           .withReplicationFactor(3)
 *           .withPartitionsCount(1)
 *           .useEmbeddedGateway(true)
 *           .withNetwork(network)
 *           .build();
 *     }
 *
 *     &#64;AfterEach
 *     void afterEach() {
 *       cluster.stop();
 *       network.close();
 *     }
 *
 *     &#64;Test
 *     void shouldConnectToCluster() {
 *       // given
 *       cluster.start();
 *
 *       // when
 *       final Topology topology;
 *       try (final ZeebeClient client = cluster.newClientBuilder().build()) {
 *         topology = c.newTopologyRequest().send().join();
 *       }
 *
 *       // then
 *       assertThat(topology.getClusterSize()).isEqualTo(3);
 *     }
 *   }
 * }
 */
@API(status = Status.EXPERIMENTAL)
@SuppressWarnings({"java:S1452", "unused"})
public class ZeebeCluster implements Startable {
  private static final Logger LOGGER = LoggerFactory.getLogger(ZeebeCluster.class);

  private final Network network;
  private final String name;
  private final Map<String, ZeebeGatewayNode<? extends GenericContainer<?>>> gateways;
  private final Map<Integer, ZeebeBrokerNode<? extends GenericContainer<?>>> brokers;
  private final int replicationFactor;
  private final int partitionsCount;

  public ZeebeCluster(
      final Network network,
      final String name,
      final Map<String, ZeebeGatewayNode<? extends GenericContainer<?>>> gateways,
      final Map<Integer, ZeebeBrokerNode<? extends GenericContainer<?>>> brokers,
      final int replicationFactor,
      final int partitionsCount) {
    this.network = network;
    this.name = name;
    this.gateways = Collections.unmodifiableMap(gateways);
    this.brokers = Collections.unmodifiableMap(brokers);
    this.replicationFactor = replicationFactor;
    this.partitionsCount = partitionsCount;
  }

  /** @return a new cluster builder */
  public static ZeebeClusterBuilder builder() {
    return new ZeebeClusterBuilder();
  }

  /**
   * Starts all containers in the cluster. This is a blocking method: it will return only when all
   * containers are marked as ready.
   *
   * <p>NOTE: although gateways could technically be started in any order, brokers
   * <strong>must</strong> be started in parallel, as they will fail to be ready if they cannot at
   * least form a Raft (during the initial startup).
   */
  @Override
  public void start() {
    // as containers are not thread safe (especially the containerId property), it's important that
    // we don't try to start the same container on different threads (i.e. start brokers, then
    // gateways), as they may end up creating multiple real containers from a single
    // GenericContainer if the containerId property isn't updated in either thread
    LOGGER.info(
        "Starting cluster {} with {} brokers, {} gateways, {} partitions, and a replication factor of {}",
        name,
        brokers.size(),
        gateways.size(),
        partitionsCount,
        replicationFactor);
    Startables.deepStart(getClusterContainers()).join();
  }

  /** Stops all containers in the cluster. */
  @Override
  public void stop() {
    // as containers are not thread safe in general, there may be a race condition when stopping
    // them on the default fork join pool if the threads from the pool haven't synchronized with
    // this one or the ones used to start the container. it could be in very rare cases that they
    // see no containerId property and stop wouldn't do anything. at any rate, since it's cheap to
    // stop containers, we can simply do it sequentially
    getClusterContainers().forEach(Startable::stop);
  }

  /** @return the network over which all containers are communicating */
  public Network getNetwork() {
    return network;
  }

  /** @return the replication factor configured for the brokers */
  public int getReplicationFactor() {
    return replicationFactor;
  }

  /** @return the partitions count configured for the brokers */
  public int getPartitionsCount() {
    return partitionsCount;
  }

  /** @return the cluster name */
  public String getName() {
    return name;
  }

  /**
   * Returns a map of the gateways in the cluster, where the keys are the memberIds, and the values
   * the gateway containers.
   *
   * <p>NOTE: this may include brokers with embedded gateways as well. To check if a node is a
   * standalone gateway or a broker, you can check if it's an instance of {@link
   * io.zeebe.containers.ZeebeGatewayContainer} or not.
   *
   * @return the gateways in this cluster
   */
  public Map<String, ZeebeGatewayNode<? extends GenericContainer<?>>> getGateways() {
    return gateways;
  }

  /**
   * Returns a map of the brokers in the cluster, where the keys are the broker's nodeId, and the
   * values the broker containers.
   *
   * @return the brokers in this cluster
   */
  public Map<Integer, ZeebeBrokerNode<? extends GenericContainer<?>>> getBrokers() {
    return brokers;
  }

  /**
   * Builds a new client builder by picking a random gateway started gateway for it and disabling
   * transport security.
   *
   * @return a new client builder with the gateway and transport security pre-configured
   * @throws NoSuchElementException if there are no started gateways
   */
  public ZeebeClientBuilder newClientBuilder() {
    final ZeebeGatewayNode<?> gateway =
        gateways.values().stream()
            .filter(ZeebeNode::isStarted)
            .findAny()
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "Expected at least one gateway for the client to connect to, but there is none"));

    return ZeebeClient.newClientBuilder()
        .gatewayAddress(gateway.getExternalGatewayAddress())
        .usePlaintext();
  }

  private Stream<? extends GenericContainer<?>> getGatewayContainers() {
    return gateways.values().stream().map(Container::self);
  }

  private Stream<? extends GenericContainer<?>> getBrokerContainers() {
    return brokers.values().stream().map(Container::self);
  }

  private Stream<GenericContainer<? extends GenericContainer<?>>> getClusterContainers() {
    return Streams.concat(getBrokerContainers(), getGatewayContainers()).distinct();
  }
}

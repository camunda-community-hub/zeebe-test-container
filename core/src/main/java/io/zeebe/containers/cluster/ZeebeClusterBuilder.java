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

import static io.zeebe.containers.ZeebeDefaults.getInstance;

import io.zeebe.containers.ZeebeBrokerContainer;
import io.zeebe.containers.ZeebeBrokerNode;
import io.zeebe.containers.ZeebeContainer;
import io.zeebe.containers.ZeebeGatewayContainer;
import io.zeebe.containers.ZeebeGatewayNode;
import io.zeebe.containers.ZeebeNode;
import io.zeebe.containers.ZeebeTopologyWaitStrategy;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

/**
 * Convenience class to help build Zeebe clusters.
 *
 * <p>The default configuration of this cluster is equivalent to starting a single {@link
 * ZeebeContainer} with all defaults, that is:
 *
 * <ul>
 *   <li>one broker in the cluster with embedded gateway
 *   <li>one partition
 *   <li>replication factor of 1
 * </ul>
 *
 * By default, all nodes will use the {@link Network#SHARED} network as well.
 *
 * <p>Example usage for a cluster with 3 brokers, 3 partitions, replication factor 3, and all
 * embedded gateways:
 *
 * <pre>{@code
 * final ZeebeCluster cluster = ZeebeCluster.builder()
 *    .withEmbeddedGateway(true)
 *    .withGatewaysCount(0)
 *    .withBrokersCount(3)
 *    .withReplicationFactor(3)
 *    .withPartitionsCount(3)
 *    .build();
 *
 * }</pre>
 *
 * <p>Example for the same as the above, but this time with one standalone gateway instead of
 * embedded gateways:
 *
 * <pre>{@code
 * final ZeebeCluster cluster = ZeebeCluster.builder()
 *    .withEmbeddedGateway(false)
 *    .withGatewaysCount(1)
 *    .withBrokersCount(3)
 *    .withReplicationFactor(3)
 *    .withPartitionsCount(3)
 *    .build();
 *
 * }</pre>
 *
 * <p>Example for the same as the above, but with a mix of gateways:
 *
 * <pre>{@code
 * final ZeebeCluster cluster = ZeebeCluster.builder()
 *    .withEmbeddedGateway(true)
 *    .withGatewaysCount(2)
 *    .withBrokersCount(3)
 *    .withReplicationFactor(3)
 *    .withPartitionsCount(3)
 *    .build();
 *
 * }</pre>
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
@API(status = Status.EXPERIMENTAL)
public class ZeebeClusterBuilder {

  private static final String BROKER_NETWORK_ALIAS_PREFIX = "zeebe-broker-";
  private static final String GATEWAY_NETWORK_ALIAS_PREFIX = "zeebe-gateway-";
  private static final String DEFAULT_CLUSTER_NAME = "zeebe-cluster";

  private Network network = Network.SHARED;
  private String name = DEFAULT_CLUSTER_NAME;

  private int gatewaysCount = 0;

  private int brokersCount = 1;
  private int partitionsCount = 1;
  private int replicationFactor = 1;
  private boolean useEmbeddedGateway = true;
  private DockerImageName gatewayImageName = getInstance().getDefaultDockerImage();
  private DockerImageName brokerImageName = getInstance().getDefaultDockerImage();

  private Consumer<ZeebeNode<?>> nodeConfig = cfg -> {};
  private BiConsumer<Integer, ZeebeBrokerNode<?>> brokerConfig = (id, cfg) -> {};
  private BiConsumer<String, ZeebeGatewayNode<?>> gatewayConfig = (memberId, cfg) -> {};

  private final Map<String, ZeebeGatewayNode<? extends GenericContainer<?>>> gateways =
      new HashMap<>();
  private final Map<Integer, ZeebeBrokerNode<? extends GenericContainer<?>>> brokers =
      new HashMap<>();

  /**
   * If true, the brokers created by this cluster will use embedded gateways. By default this is
   * true.
   *
   * @param useEmbeddedGateway true or false to enable the embedded gateway on the brokers
   * @return this builder instance for chaining
   */
  public ZeebeClusterBuilder withEmbeddedGateway(final boolean useEmbeddedGateway) {
    this.useEmbeddedGateway = useEmbeddedGateway;
    return this;
  }

  /**
   * The number of standalone gateway to create in this cluster. By default, this is 0, and the
   * brokers have embedded gateways.
   *
   * <p>Note that this parameter has no impact on the use of embedded gateways, and a cluster can
   * contain both standalone and embedded gateways.
   *
   * @param gatewaysCount the number of standalone gateways to create
   * @return this builder instance for chaining
   */
  public ZeebeClusterBuilder withGatewaysCount(final int gatewaysCount) {
    this.gatewaysCount = gatewaysCount;
    return this;
  }

  /**
   * The number of brokers to create in this cluster. By default, this is 1. Any gateway part of
   * this cluster will use this number as well for its {@link
   * ZeebeTopologyWaitStrategy#forBrokersCount(int)}.
   *
   * <p>Note that it's possible to create a cluster with no brokers, as this is may be a valid set
   * up for testing purposes. If that's the case, the gateways will not wait for the topology to be
   * complete (as they cannot know the topology), and will not be configured with a contact point.
   *
   * <p>NOTE: setting this to 0 will also set the replication factor and partitions count to 0.
   *
   * @param brokersCount the number of brokers to create
   * @return this builder instance for chaining
   */
  public ZeebeClusterBuilder withBrokersCount(final int brokersCount) {
    if (brokersCount < 0) {
      throw new IllegalArgumentException(
          "Expected brokersCount to be at least 0, but was " + brokersCount);
    }

    this.brokersCount = brokersCount;
    if (brokersCount > 0) {
      partitionsCount = Math.max(partitionsCount, 1);
      replicationFactor = Math.max(replicationFactor, 1);
    } else {
      partitionsCount = 0;
      replicationFactor = 0;
    }

    return this;
  }

  /**
   * Will set the number of partitions to distribute across the cluster. For example, if there are 3
   * brokers, 3 partitions, but replication factor of 1, then each broker will get exactly one
   * partition. Any gateway in the cluster will also use this number for its {@link
   * ZeebeTopologyWaitStrategy#forPartitionsCount(int)}.
   *
   * <p>Note that the number of partitions must be greater than or equal to 1! If you do not want to
   * have any brokers, then set {@link #withBrokersCount(int)} to 0 instead.
   *
   * @param partitionsCount the number of partitions to distribute across the cluster
   * @return this builder instance for chaining
   */
  public ZeebeClusterBuilder withPartitionsCount(final int partitionsCount) {
    if (partitionsCount <= 0) {
      throw new IllegalArgumentException(
          "Expected partitionsCount to be at least 1, but was " + partitionsCount);
    }

    this.partitionsCount = partitionsCount;
    return this;
  }

  /**
   * Sets the replication factor for each partition in the cluster. Note that this cannot be less
   * than 1, or greater than the broker count (see {@link #withBrokersCount(int)}).
   *
   * @param replicationFactor the replication factor for each partition
   * @return this builder instance for chaining
   */
  public ZeebeClusterBuilder withReplicationFactor(final int replicationFactor) {
    if (replicationFactor <= 0) {
      throw new IllegalArgumentException(
          "Expected replicationFactor to be at least 1, but was " + replicationFactor);
    }

    this.replicationFactor = replicationFactor;
    return this;
  }

  /**
   * Sets the network the containers should use to communicate between each other. It's recommended
   * to create a new network if you have more than one or two nodes, or if you're running multiple
   * tests in parallel.
   *
   * <p>By default, will use {@link Network#SHARED}.
   *
   * @param network the network containers will use to communicate between each other
   * @return this builder instance for chaining
   */
  public ZeebeClusterBuilder withNetwork(final Network network) {
    this.network = Objects.requireNonNull(network);
    return this;
  }

  /**
   * Sets the name of the cluster. This can be used to prevent nodes in one cluster from
   * inadvertently communicating with nodes in another cluster.
   *
   * <p>Unless you're deploying multiple clusters in the same network, leave as is.
   *
   * @param name the cluster name
   * @return this builder instance for chaining
   */
  public ZeebeClusterBuilder withName(final String name) {
    if (name == null || name.trim().length() < 3) {
      throw new IllegalArgumentException(
          "Expected cluster name to be at least 3 characters, but was " + name);
    }

    this.name = name;
    return this;
  }

  /**
   * Sets the docker image for separate gateways. This could be used to create a test against
   * specific Zeebe version.
   *
   * @param gatewayImageName the docker image name of the Zeebe
   * @return this builder instance for chaining
   */
  public ZeebeClusterBuilder withGatewayImage(final DockerImageName gatewayImageName) {
    this.gatewayImageName = Objects.requireNonNull(gatewayImageName);
    return this;
  }

  /**
   * Sets the docker image for brokers with embedded gateways. This could be used to create a test
   * against specific Zeebe version.
   *
   * @param brokerImageName the docker image name of the Zeebe
   * @return this builder instance for chaining
   */
  public ZeebeClusterBuilder withBrokerImage(final DockerImageName brokerImageName) {
    this.brokerImageName = Objects.requireNonNull(brokerImageName);
    return this;
  }

  /**
   * Sets the docker image for brokers with embedded gateways and separate gateways. This could be
   * used to create a test against specific Zeebe version.
   *
   * @param imageName the docker image name of the Zeebe
   * @return this builder instance for chaining
   */
  public ZeebeClusterBuilder withImage(final DockerImageName imageName) {
    return withGatewayImage(imageName).withBrokerImage(imageName);
  }

  /**
   * Sets the configuration function that will be executed in the {@link #build()} method on both
   * brokers and gateways (embedded gateways included). NOTE: this configuration has the lowest
   * priority, e.g. other configurations ({@link #gatewayConfig} or {@link #brokerConfig}) will
   * override this configuration in case of conflicts.
   *
   * @param nodeCfgFunction the function that will be applied on all cluster nodes
   * @return this builder instance for chaining
   */
  public ZeebeClusterBuilder withNodeConfig(final Consumer<ZeebeNode<?>> nodeCfgFunction) {
    this.nodeConfig = nodeCfgFunction;
    return this;
  }

  /**
   * Sets the configuration function that will be executed in the {@link #build()} method on each
   * gateway (including embedded gateways). The first argument of is the member ID of the gateway,
   * and the second argument is the gateway container itself.
   *
   * <p>NOTE: in case of conflicts with {@link #nodeConfig} this configuration will override {@link
   * #nodeConfig}.
   *
   * <p>NOTE: in case of conflicts with this configuration is an embedded gateway configuration and
   * a broker configuration, broker configuration will override this configuration.
   *
   * @param gatewayCfgFunction the function that will be applied on all cluster gateways (embedded
   *     ones included)
   * @return this builder instance for chaining
   */
  public ZeebeClusterBuilder withGatewayConfig(
      final BiConsumer<String, ZeebeGatewayNode<?>> gatewayCfgFunction) {
    this.gatewayConfig = gatewayCfgFunction;
    return this;
  }

  /**
   * Sets the configuration function that will be executed in the {@link #build()} method on each
   * gateway (including embedded gateways).
   *
   * <p>NOTE: in case of conflicts with {@link #nodeConfig} this configuration will override {@link
   * #nodeConfig}.
   *
   * <p>NOTE: in case of conflicts with this configuration is an embedded gateway configuration and
   * a broker configuration, broker configuration will override this configuration.
   *
   * @param gatewayCfgFunction the function that will be applied on all cluster gateways (embedded
   *     ones included)
   * @return this builder instance for chaining
   */
  public ZeebeClusterBuilder withGatewayConfig(
      final Consumer<ZeebeGatewayNode<?>> gatewayCfgFunction) {
    this.gatewayConfig = (memberId, gateway) -> gatewayCfgFunction.accept(gateway);
    return this;
  }

  /**
   * Sets the configuration function that will be executed in the {@link #build()} method on each
   * broker. The first argument is the broker ID, and the second argument is the broker container
   * itself.
   *
   * <p>NOTE: in case of conflicts with {@link #nodeConfig} or {@link #gatewayConfig} this
   * configuration will override them.
   *
   * @param brokerCfgFunction the function that will be applied on all cluster brokers
   * @return this builder instance for chaining
   */
  public ZeebeClusterBuilder withBrokerConfig(
      final BiConsumer<Integer, ZeebeBrokerNode<?>> brokerCfgFunction) {
    this.brokerConfig = brokerCfgFunction;
    return this;
  }

  /**
   * Sets the configuration function that will be executed in the {@link #build()} method on each
   * broker.
   *
   * <p>NOTE: in case of conflicts with {@link #nodeConfig} or {@link #gatewayConfig} this
   * configuration will override them.
   *
   * @param brokerCfgFunction the function that will be applied on all cluster brokers
   * @return this builder instance for chaining
   */
  public ZeebeClusterBuilder withBrokerConfig(
      final Consumer<ZeebeBrokerNode<?>> brokerCfgFunction) {
    this.brokerConfig = (id, broker) -> brokerCfgFunction.accept(broker);
    return this;
  }

  /**
   * Builds a new Zeebe cluster. Will create {@link #brokersCount} brokers (accessible later via
   * {@link ZeebeCluster#getBrokers()}) and {@link #gatewaysCount} standalone gateways (accessible
   * later via {@link ZeebeCluster#getGateways()}).
   *
   * <p>If {@link #useEmbeddedGateway} is true, then all brokers will have the embedded gateway
   * enabled and the right topology check configured. Additionally, {@link
   * ZeebeCluster#getGateways()} will also include them, along with any other additional standalone
   * gateway.
   *
   * <p>NOTE: as a rule of thumb, we had one minute to the startup timeout for each node in the
   * cluster. For example, if you have 2 gateways and 3 brokers, each container will have a maximum
   * startup time of 5 minutes. You can still change that by configuring the containers manually
   * after building but before starting the cluster.
   *
   * <p>For standalone gateways, if {@link #brokersCount} is at least one, then a random broker is
   * picked as the contact point for all gateways.
   *
   * @return a new Zeebe cluster
   */
  public ZeebeCluster build() {
    gateways.clear();
    brokers.clear();

    validate();
    createBrokers();

    // gateways are configured after brokers such that we can set the right contact point if there
    // is one
    createStandaloneGateways();

    // apply free-form configuration functions
    brokers.forEach(this::applyConfigFunctions);
    gateways.forEach(
        (memberId, gateway) -> {
          // skip brokers/embedded gateways
          if (!(gateway instanceof ZeebeBrokerNode<?>)) {
            applyConfigFunctions(memberId, gateway);
          }
        });

    return new ZeebeCluster(network, name, gateways, brokers, replicationFactor, partitionsCount);
  }

  private void applyConfigFunctions(final Object id, final ZeebeNode<?> node) {
    nodeConfig.accept(node);

    if (node instanceof ZeebeGatewayNode) {
      gatewayConfig.accept(String.valueOf(id), (ZeebeGatewayNode<?>) node);
    }

    if (node instanceof ZeebeBrokerNode) {
      brokerConfig.accept((Integer) id, (ZeebeBrokerNode<?>) node);
    }
  }

  private void validate() {
    if (replicationFactor > brokersCount) {
      throw new IllegalStateException(
          "Expected replicationFactor to be less than or equal to brokersCount, but was "
              + replicationFactor
              + " > "
              + brokersCount);
    }

    if (brokersCount > 0) {
      if (partitionsCount < 1) {
        throw new IllegalStateException(
            "Expected to have at least one partition if there are any brokers, but partitionsCount"
                + " was "
                + partitionsCount);
      }

      if (replicationFactor < 1) {
        throw new IllegalStateException(
            "Expected to have replication factor at least 1 if there are any brokers, but"
                + " replicationFactor was "
                + replicationFactor);
      }
    }
  }

  private void createBrokers() {
    for (int i = 0; i < brokersCount; i++) {
      final ZeebeBrokerNode<?> broker;

      if (useEmbeddedGateway) {
        final ZeebeContainer container = new ZeebeContainer(brokerImageName);
        configureGateway(container);

        broker = container;
        gateways.put(String.valueOf(i), container);
      } else {
        broker = new ZeebeBrokerContainer(brokerImageName);
      }

      configureBroker(broker, i);
      brokers.put(i, broker);
    }

    // since initial contact points has to container all known brokers, we can only configure it
    // AFTER the base broker configuration
    configureBrokerInitialContactPoints();
  }

  private void createStandaloneGateways() {
    final ThreadLocalRandom random = ThreadLocalRandom.current();
    for (int i = 0; i < gatewaysCount; i++) {
      final String memberId = GATEWAY_NETWORK_ALIAS_PREFIX + i;
      //noinspection resource
      final ZeebeGatewayContainer gateway = createStandaloneGateway(memberId);
      gateway.withStartupTimeout(Duration.ofMinutes((long) gatewaysCount + brokersCount));

      if (brokersCount > 0) {
        final ZeebeBrokerNode<?> contactPoint = brokers.get(random.nextInt(0, brokers.size()));
        gateway
            .dependsOn(contactPoint.self())
            .withEnv(
                "ZEEBE_GATEWAY_CLUSTER_CONTACTPOINT", contactPoint.getInternalClusterAddress());
      }
    }
  }

  private ZeebeGatewayContainer createStandaloneGateway(final String memberId) {
    final ZeebeGatewayContainer gateway = new ZeebeGatewayContainer(gatewayImageName);

    gateway
        .withNetwork(network)
        .withNetworkAliases(memberId)
        .withEnv("ZEEBE_GATEWAY_CLUSTER_CLUSTERNAME", name)
        .withEnv("ZEEBE_GATEWAY_CLUSTER_HOST", gateway.getInternalHost())
        .withEnv("ZEEBE_GATEWAY_CLUSTER_MEMBERID", memberId);

    configureGateway(gateway);
    gateways.put(memberId, gateway);
    return gateway;
  }

  private void configureGateway(final ZeebeGatewayNode<?> gateway) {
    gateway.withTopologyCheck(
        new ZeebeTopologyWaitStrategy()
            .forBrokersCount(brokersCount)
            .forPartitionsCount(partitionsCount)
            .forReplicationFactor(replicationFactor));
  }

  private void configureBroker(final ZeebeBrokerNode<?> broker, final int index) {
    final String hostName = BROKER_NETWORK_ALIAS_PREFIX + index;

    broker
        .withNetwork(network)
        .withNetworkAliases(hostName)
        .withEnv("ZEEBE_BROKER_NETWORK_ADVERTISEDHOST", broker.getInternalHost())
        .withEnv("ZEEBE_BROKER_CLUSTER_CLUSTERNAME", name)
        .withEnv("ZEEBE_BROKER_CLUSTER_NODEID", String.valueOf(index))
        .withEnv("ZEEBE_BROKER_CLUSTER_CLUSTERSIZE", String.valueOf(brokersCount))
        .withEnv("ZEEBE_BROKER_CLUSTER_REPLICATIONFACTOR", String.valueOf(replicationFactor))
        .withEnv("ZEEBE_BROKER_CLUSTER_PARTITIONSCOUNT", String.valueOf(partitionsCount))
        .withStartupTimeout(Duration.ofMinutes((long) brokersCount + gatewaysCount));
  }

  private void configureBrokerInitialContactPoints() {
    final String initialContactPoints =
        brokers.values().stream()
            .map(ZeebeBrokerNode::getInternalClusterAddress)
            .collect(Collectors.joining(","));
    brokers
        .values()
        .forEach(b -> b.withEnv("ZEEBE_BROKER_CLUSTER_INITIALCONTACTPOINTS", initialContactPoints));
  }
}

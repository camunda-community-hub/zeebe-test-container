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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.entry;

import io.zeebe.containers.ZeebeBrokerNode;
import io.zeebe.containers.ZeebeGatewayNode;
import io.zeebe.containers.ZeebeNode;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

final class ZeebeClusterBuilderTest {
  @Test
  void shouldThrowIllegalArgumentIfBrokersCountIsNegative() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // then
    assertThatCode(() -> builder.withBrokersCount(-1))
        .as("the builder should not accept a negative number of brokers")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldThrowIllegalArgumentWhenPartitionsIsNotStrictlyPositive() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // then
    assertThatCode(() -> builder.withPartitionsCount(0))
        .as("the builder should not accept no partitions")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatCode(() -> builder.withPartitionsCount(-1))
        .as("the builder should not accept a negative partitions count")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldThrowIllegalArgumentIfReplicationFactorIsNotStrictlyPositive() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // then
    assertThatCode(() -> builder.withReplicationFactor(0))
        .as("the builder should not accept 0 as a replication factor")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatCode(() -> builder.withReplicationFactor(-1))
        .as("the builder should not accept a negative replication factor")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldThrowNullExceptionIfNetworkIsNull() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // then
    assertThatCode(() -> builder.withNetwork(null))
        .as("the builder should not accept a null network")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenNameIsNull() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // then
    assertThatCode(() -> builder.withName(null))
        .as("the builder should not accept a null cluster name")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenNameIsTooShort() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // then
    assertThatCode(() -> builder.withName(""))
        .as("the builder should not accept an empty cluster name")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatCode(() -> builder.withName("a"))
        .as("the builder should not accept a name which is too short")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatCode(() -> builder.withName("aa"))
        .as("the builder should not accept a name which is too short")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldThrowIllegalStateExceptionWhenReplicationFactorIsGreaterThanBrokersCount() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // when
    builder.withBrokersCount(1).withReplicationFactor(2);

    // then
    assertThatCode(builder::build)
        .as(
            "the builder should not accept a replication factor which is greater than the number of"
                + " brokers")
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void shouldRespectBrokersCount() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // when
    builder.withBrokersCount(2);
    final ZeebeCluster cluster = builder.build();

    // then
    final Map<Integer, ZeebeBrokerNode<? extends GenericContainer<?>>> brokers =
        cluster.getBrokers();
    assertThat(brokers)
        .as("the builder created 2 brokers with the right IDs")
        .hasSize(2)
        .containsKeys(0, 1);
    assertThat(brokers.get(0).getEnvMap())
        .as("the first broker has ID 0 and the right cluster size")
        .contains(
            entry("ZEEBE_BROKER_CLUSTER_NODEID", "0"),
            entry("ZEEBE_BROKER_CLUSTER_CLUSTERSIZE", "2"));
    assertThat(brokers.get(1).getEnvMap())
        .as("the first broker has ID 1 and the right cluster size")
        .contains(
            entry("ZEEBE_BROKER_CLUSTER_NODEID", "1"),
            entry("ZEEBE_BROKER_CLUSTER_CLUSTERSIZE", "2"));
  }

  @Test
  void shouldZeroPartitionsAndReplicationFactorIfBrokersCountIsZero() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // when
    builder.withBrokersCount(0);
    final ZeebeCluster cluster = builder.build();

    // then
    assertThat(cluster.getReplicationFactor())
        .as("there are no replication factor if no brokers are defined")
        .isZero();
    assertThat(cluster.getPartitionsCount())
        .as("there are no partitions if no brokers are defined")
        .isZero();
  }

  @Test
  void shouldResetPartitionsAndReplicationFactorToOneIfBrokersCountGoesFromZeroToPositive() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // when
    builder.withBrokersCount(0).withBrokersCount(3);
    final ZeebeCluster cluster = builder.build();

    // then
    assertThat(cluster.getReplicationFactor())
        .as("there are is a default replication factor when the broker count is redefined")
        .isEqualTo(1);
    assertThat(cluster.getPartitionsCount())
        .as("there are is a default partitions count when the broker count is redefined")
        .isEqualTo(1);
  }

  @Test
  void shouldNotModifyPartitionsCountOrReplicationFactoryWhenSettingBrokersCount() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // when
    builder.withPartitionsCount(4).withReplicationFactor(1).withBrokersCount(3);
    final ZeebeCluster cluster = builder.build();

    // then
    assertThat(cluster.getReplicationFactor())
        .as("the replication factor should be static even when the brokers count is changed after")
        .isEqualTo(1);
    assertThat(cluster.getPartitionsCount())
        .as("the partitions should be static even when the brokers count is changed after")
        .isEqualTo(4);
  }

  @Test
  void shouldRespectPartitionsCount() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // when
    builder.withPartitionsCount(2);
    final ZeebeCluster cluster = builder.build();

    // then
    final Map<Integer, ZeebeBrokerNode<? extends GenericContainer<?>>> brokers =
        cluster.getBrokers();
    assertThat(cluster.getPartitionsCount())
        .as("the configure the partitions count correctly")
        .isEqualTo(2);
    assertThat(brokers.get(0).getEnvMap())
        .as("the broker should report the correct environment variable as config")
        .containsEntry("ZEEBE_BROKER_CLUSTER_PARTITIONSCOUNT", "2");
  }

  @Test
  void shouldRespectReplicationFactor() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // when
    builder.withBrokersCount(2).withReplicationFactor(2);
    final ZeebeCluster cluster = builder.build();

    // then
    final Map<Integer, ZeebeBrokerNode<? extends GenericContainer<?>>> brokers =
        cluster.getBrokers();
    assertThat(cluster.getReplicationFactor())
        .as("the broker should report the correct replication factor")
        .isEqualTo(2);
    assertThat(brokers.get(0).getEnvMap())
        .as("the first broker should report the correct environment variable as config")
        .containsEntry("ZEEBE_BROKER_CLUSTER_REPLICATIONFACTOR", "2");
    assertThat(brokers.get(1).getEnvMap())
        .as("the second broker should report the correct environment variable as config")
        .containsEntry("ZEEBE_BROKER_CLUSTER_REPLICATIONFACTOR", "2");
  }

  @Test
  void shouldAssignDifferentInternalHostNamesToEveryNode() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // when
    builder.withEmbeddedGateway(false).withBrokersCount(2).withGatewaysCount(2);
    final ZeebeCluster cluster = builder.build();

    // then
    final Set<String> internalHosts = new HashSet<>();
    cluster.getBrokers().values().stream()
        .map(ZeebeNode::getInternalHost)
        .forEach(internalHosts::add);
    cluster.getGateways().values().stream()
        .map(ZeebeNode::getInternalHost)
        .forEach(internalHosts::add);
    assertThat(internalHosts)
        .as("every node in the cluster has a unique internal host name")
        .hasSize(4)
        .doesNotContainNull();
  }

  @Test
  void shouldAssignDifferentClusterHostsToAllNodes() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // when
    builder.withEmbeddedGateway(false).withBrokersCount(2).withGatewaysCount(2);
    final ZeebeCluster cluster = builder.build();

    // then
    final Set<String> advertisedHosts = new HashSet<>();
    cluster.getBrokers().values().stream()
        .map(Container::getEnvMap)
        .map(env -> env.get("ZEEBE_BROKER_NETWORK_ADVERTISEDHOST"))
        .forEach(advertisedHosts::add);
    cluster.getGateways().values().stream()
        .map(Container::getEnvMap)
        .map(env -> env.get("ZEEBE_GATEWAY_CLUSTER_HOST"))
        .forEach(advertisedHosts::add);
    assertThat(advertisedHosts)
        .as("every node in the cluster has a unique advertised host")
        .hasSize(4)
        .doesNotContainNull();
  }

  @Test
  void shouldRespectClusterName() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // when
    builder
        .withName("test-cluster")
        .withEmbeddedGateway(false)
        .withBrokersCount(2)
        .withGatewaysCount(2);
    final ZeebeCluster cluster = builder.build();

    // then

    cluster
        .getBrokers()
        .values()
        .forEach(
            b ->
                assertThat(b.getEnvMap())
                    .as("every broker is configured with the correct cluster name")
                    .containsEntry("ZEEBE_BROKER_CLUSTER_CLUSTERNAME", "test-cluster"));
    cluster
        .getGateways()
        .values()
        .forEach(
            g ->
                assertThat(g.getEnvMap())
                    .as("every gateway is configured with the correct cluster name")
                    .containsEntry("ZEEBE_GATEWAY_CLUSTER_CLUSTERNAME", "test-cluster"));
  }

  @Test
  void shouldRespectNetwork() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();
    final Network network = Network.newNetwork();

    // when
    builder
        .withNetwork(network)
        .withEmbeddedGateway(false)
        .withBrokersCount(2)
        .withGatewaysCount(2);
    final ZeebeCluster cluster = builder.build();

    // then
    assertThat(cluster.getBrokers()).hasSize(2);
    assertThat(cluster.getGateways()).hasSize(2);

    cluster
        .getBrokers()
        .values()
        .forEach(
            b ->
                assertThat(b.self().getNetwork())
                    .as("every broker is configured with the correct network")
                    .isEqualTo(network));
    cluster
        .getGateways()
        .values()
        .forEach(
            g ->
                assertThat(g.self().getNetwork())
                    .as("every gateway is configured with the correct network")
                    .isEqualTo(network));
  }

  @Test
  void shouldAssignUniqueMemberIdToEachGateway() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // when
    builder.withEmbeddedGateway(false).withGatewaysCount(2);
    final ZeebeCluster cluster = builder.build();

    // then
    final Set<String> memberIds = cluster.getGateways().keySet();
    assertThat(memberIds).hasSize(2);
    for (final String memberId : memberIds) {
      final ZeebeGatewayNode<? extends GenericContainer<?>> gateway =
          cluster.getGateways().get(memberId);
      assertThat(gateway.getEnvMap())
          .as("every gateway has a unique member configured via environment variable")
          .containsEntry("ZEEBE_GATEWAY_CLUSTER_MEMBERID", memberId);
    }
  }

  @Test
  void shouldAssignUniqueNodeIdToEachBroker() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // when
    builder.withEmbeddedGateway(false).withBrokersCount(2);
    final ZeebeCluster cluster = builder.build();

    // then
    final Set<Integer> nodeIds = cluster.getBrokers().keySet();
    assertThat(nodeIds).hasSize(2);
    for (final Integer nodeId : nodeIds) {
      final ZeebeBrokerNode<? extends GenericContainer<?>> broker =
          cluster.getBrokers().get(nodeId);
      assertThat(broker.getEnvMap())
          .as("every broker has a unique node ID configured via environment variable")
          .containsEntry("ZEEBE_BROKER_CLUSTER_NODEID", String.valueOf(nodeId));
    }
  }

  @Test
  void shouldAssignAllBrokersAsInitialContactPoints() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // when
    builder.withEmbeddedGateway(false).withBrokersCount(2);
    final ZeebeCluster cluster = builder.build();

    // then
    final Map<Integer, ZeebeBrokerNode<? extends GenericContainer<?>>> brokers =
        cluster.getBrokers();
    final String brokerZeroInitialContactPoints =
        brokers.get(0).getEnvMap().get("ZEEBE_BROKER_CLUSTER_INITIALCONTACTPOINTS");
    final String brokerOneInitialContactPoints =
        brokers.get(1).getEnvMap().get("ZEEBE_BROKER_CLUSTER_INITIALCONTACTPOINTS");

    assertThat(brokerZeroInitialContactPoints)
        .as(
            "both broker 0 and broker 1 report each other as initial contact points via environment"
                + " variables")
        .isEqualTo(brokerOneInitialContactPoints)
        .containsOnlyOnce(brokers.get(0).getInternalClusterAddress())
        .containsOnlyOnce(brokers.get(1).getInternalClusterAddress());
  }

  @Test
  void shouldAssignABrokerAsContactPointForStandaloneGateway() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // when
    builder.withEmbeddedGateway(false).withGatewaysCount(1).withBrokersCount(1);
    final ZeebeCluster cluster = builder.build();

    // then
    final ZeebeBrokerNode<? extends GenericContainer<?>> broker = cluster.getBrokers().get(0);
    final ZeebeGatewayNode<? extends GenericContainer<?>> gateway =
        cluster.getGateways().values().iterator().next();

    assertThat(gateway.getEnvMap())
        .as("the gateway has the correct broker contact point configured")
        .containsEntry("ZEEBE_GATEWAY_CLUSTER_CONTACTPOINT", broker.getInternalClusterAddress());
  }

  @Test
  void shouldNotAssignContactPointToStandaloneGatewayIfNoBrokersAvailable() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // when
    builder.withGatewaysCount(1).withBrokersCount(0);
    final ZeebeCluster cluster = builder.build();

    // then
    final ZeebeGatewayNode<? extends GenericContainer<?>> gateway =
        cluster.getGateways().values().iterator().next();

    assertThat(gateway.getEnvMap())
        .as("the gateway has no contact point configured since there are no brokers")
        .doesNotContainKey("ZEEBE_GATEWAY_CLUSTER_CONTACTPOINT");
  }

  @Test
  void shouldConfigureEmbeddedGateway() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // when
    builder.withEmbeddedGateway(true).withBrokersCount(1);
    final ZeebeCluster cluster = builder.build();

    // then
    assertThat(cluster.getGateways())
        .as("there is a gateway even if there is only a single node")
        .hasSize(1);
    assertThat(cluster.getGateways().get("0"))
        .as("the gateway is actual a broker as it is an embedded gateway")
        .isInstanceOf(ZeebeBrokerNode.class);
    assertThat(cluster.getGateways().get("0").getEnvMap())
        .as("the broker is configured to enable the embedded gateway")
        .containsEntry("ZEEBE_BROKER_GATEWAY_ENABLE", "true");
  }

  @Test
  void shouldNotConfigureEmbeddedGateway() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // when
    builder.withEmbeddedGateway(false).withBrokersCount(1);
    final ZeebeCluster cluster = builder.build();

    // then
    assertThat(cluster.getGateways()).as("there are no configured gateways").isEmpty();
    assertThat(cluster.getBrokers().get(0).getEnvMap())
        .as("the broker is not configured to enable the embedded gateway")
        .doesNotContainEntry("ZEEBE_BROKER_GATEWAY_ENABLE", "true");
  }

  @Test
  void shouldApplyBrokerConfigurationOnlyOnBrokers() {
    // given
    final String foreseeEnv = "IS_CONFIGURED_BY_BROKER_FUNCTION";
    final ZeebeClusterBuilder builder =
        new ZeebeClusterBuilder()
            .withBrokerConfig(broker -> broker.addEnv(foreseeEnv, ""))
            .withBrokersCount(1)
            .withGatewaysCount(1)
            .withEmbeddedGateway(false);

    // when
    final ZeebeCluster cluster = builder.build();

    // then
    assertThat(cluster.getBrokers())
        .as(
            "all brokers must have %s environment variable, e.g. must be configured by function",
            foreseeEnv)
        .allSatisfy(
            (integer, zeebeBrokerNode) ->
                assertThat(zeebeBrokerNode.getEnvMap())
                    .as(
                        "Broker node: %s must have %s environment variable",
                        zeebeBrokerNode, foreseeEnv)
                    .containsKey(foreseeEnv));
    assertThat(cluster.getGateways())
        .as(
            "all gateways must not have %s environment variable, e.g. must not configured by function",
            foreseeEnv)
        .allSatisfy(
            (s, zeebeGatewayNode) ->
                assertThat(zeebeGatewayNode.getEnvMap())
                    .as(
                        "Gateway node: %s must not have %s environment variable",
                        zeebeGatewayNode, foreseeEnv)
                    .doesNotContainKey(foreseeEnv));
  }

  @Test
  void shouldApplyNodeConfigurationOnAllNodes() {
    // given
    final String foreseeEnv = "IS_CONFIGURED_BY_NODE_FUNCTION";
    final ZeebeClusterBuilder builder =
        new ZeebeClusterBuilder()
            .withNodeConfig(node -> node.addEnv(foreseeEnv, ""))
            .withBrokersCount(1)
            .withGatewaysCount(1)
            .withEmbeddedGateway(false);

    // when
    final ZeebeCluster cluster = builder.build();

    // then
    assertThat(cluster.getBrokers())
        .as(
            "all brokers must have %s environment variable, e.g. configured by function",
            foreseeEnv)
        .allSatisfy(
            (integer, zeebeBrokerNode) ->
                assertThat(zeebeBrokerNode.getEnvMap())
                    .as(
                        "Broker node: %s must have %s environment variable",
                        zeebeBrokerNode, foreseeEnv)
                    .containsKey(foreseeEnv));
    assertThat(cluster.getGateways())
        .as(
            "all gateways must have %s environment variable, e.g. configured by function",
            foreseeEnv)
        .allSatisfy(
            (integer, zeebeGatewayNode) ->
                assertThat(zeebeGatewayNode.getEnvMap())
                    .as(
                        "Gateway node: %s must have %s environment variable",
                        zeebeGatewayNode, foreseeEnv)
                    .containsKey(foreseeEnv));
  }

  @Test
  void shouldApplyGatewayConfigurationOnEmbeddedGateways() {
    // given
    final String foreseeEnv = "IS_CONFIGURED_BY_GATEWAY_FUNCTION";
    final ZeebeClusterBuilder builder =
        new ZeebeClusterBuilder()
            .withGatewayConfig(gateway -> gateway.addEnv(foreseeEnv, ""))
            .withBrokersCount(1)
            .withGatewaysCount(1)
            .withEmbeddedGateway(true);

    // when
    final ZeebeCluster cluster = builder.build();

    // then
    assertThat(cluster.getBrokers())
        .as(
            "all brokers must have %s environment variable, e.g. must be configured by function because they are have embedded gateways",
            foreseeEnv)
        .allSatisfy(
            (integer, zeebeBrokerNode) ->
                assertThat(zeebeBrokerNode.getEnvMap())
                    .as(
                        "Broker node: %s must have %s environment variable",
                        zeebeBrokerNode, foreseeEnv)
                    .containsKey(foreseeEnv));
    assertThat(cluster.getGateways())
        .as(
            "all gateways must have %s environment variable, e.g. must be configured by function",
            foreseeEnv)
        .allSatisfy(
            (s, zeebeGatewayNode) ->
                assertThat(zeebeGatewayNode.getEnvMap())
                    .as(
                        "Gateway node: %s must have %s environment variable",
                        zeebeGatewayNode, foreseeEnv)
                    .containsKey(foreseeEnv));
  }

  @Test
  void shouldApplyGatewayConfigurationOnlyOnGateways() {
    // given
    final String foreseeEnv = "IS_CONFIGURED_BY_GATEWAY_FUNCTION";
    final ZeebeClusterBuilder builder =
        new ZeebeClusterBuilder()
            .withGatewayConfig(gateway -> gateway.addEnv(foreseeEnv, ""))
            .withBrokersCount(1)
            .withGatewaysCount(1)
            .withEmbeddedGateway(false);

    // when
    final ZeebeCluster cluster = builder.build();

    // then
    assertThat(cluster.getBrokers())
        .as(
            "all brokers must not have %s environment variable, e.g. must not be configured by function",
            foreseeEnv)
        .allSatisfy(
            (integer, zeebeBrokerNode) ->
                assertThat(zeebeBrokerNode.getEnvMap())
                    .as(
                        "Broker node: %s must not have %s environment variable",
                        zeebeBrokerNode, foreseeEnv)
                    .doesNotContainKey(foreseeEnv));
    assertThat(cluster.getGateways())
        .as(
            "all gateways must have %s environment variable, e.g. must be configured by function",
            foreseeEnv)
        .allSatisfy(
            (s, zeebeGatewayNode) ->
                assertThat(zeebeGatewayNode.getEnvMap())
                    .as(
                        "Gateway node: %s must have %s environment variable",
                        zeebeGatewayNode, foreseeEnv)
                    .containsKey(foreseeEnv));
  }

  @Test
  void shouldBrokerConfigurationOverrideNodeConfiguration() {
    // given
    final String foreseeEnv = "IS_CONFIGURED";
    final String nodeValue = "NODE";
    final String brokerValue = "BROKER";

    final ZeebeClusterBuilder builder =
        new ZeebeClusterBuilder()
            .withNodeConfig(node -> node.addEnv(foreseeEnv, nodeValue))
            .withBrokerConfig(broker -> broker.addEnv(foreseeEnv, brokerValue))
            .withBrokersCount(1)
            .withGatewaysCount(1)
            .withEmbeddedGateway(false);

    // when
    final ZeebeCluster cluster = builder.build();

    // then
    assertThat(cluster.getBrokers())
        .as(
            "all brokers must have %s environment variable with %s value, e.g. must be configured by broker function",
            foreseeEnv, brokerValue)
        .allSatisfy(
            (integer, zeebeBrokerNode) ->
                assertThat(zeebeBrokerNode.getEnvMap())
                    .as(
                        "Broker node: %s must not have %s environment variable",
                        zeebeBrokerNode, foreseeEnv)
                    .containsEntry(foreseeEnv, brokerValue));
    assertThat(cluster.getGateways())
        .as(
            "all gateways must have %s environment variable with %s value, e.g. must be configured by node function",
            foreseeEnv, nodeValue)
        .allSatisfy(
            (s, zeebeGatewayNode) ->
                assertThat(zeebeGatewayNode.getEnvMap())
                    .as(
                        "Gateway node: %s must have %s environment variable",
                        zeebeGatewayNode, foreseeEnv)
                    .containsEntry(foreseeEnv, nodeValue));
  }

  @Test
  void shouldGatewayConfigurationOverrideNodeConfiguration() {
    // given
    final String foreseeEnv = "IS_CONFIGURED";
    final String nodeValue = "NODE";
    final String gatewayValue = "GATEWAY";

    final ZeebeClusterBuilder builder =
        new ZeebeClusterBuilder()
            .withNodeConfig(node -> node.addEnv(foreseeEnv, nodeValue))
            .withGatewayConfig(gateway -> gateway.addEnv(foreseeEnv, gatewayValue))
            .withBrokersCount(1)
            .withGatewaysCount(1)
            .withEmbeddedGateway(false);

    // when
    final ZeebeCluster cluster = builder.build();

    // then
    assertThat(cluster.getBrokers())
        .as(
            "all brokers must have %s environment variable with %s value, e.g. must not be configured by gateway function",
            foreseeEnv, gatewayValue)
        .allSatisfy(
            (integer, zeebeBrokerNode) ->
                assertThat(zeebeBrokerNode.getEnvMap())
                    .as(
                        "Broker node: %s must not have %s environment variable",
                        zeebeBrokerNode, foreseeEnv)
                    .containsEntry(foreseeEnv, nodeValue));
    assertThat(cluster.getGateways())
        .as(
            "all gateways must have %s environment variable with %s value, e.g. must be configured by gateway function",
            foreseeEnv, nodeValue)
        .allSatisfy(
            (s, zeebeGatewayNode) ->
                assertThat(zeebeGatewayNode.getEnvMap())
                    .as(
                        "Gateway node: %s must have %s environment variable",
                        zeebeGatewayNode, foreseeEnv)
                    .containsEntry(foreseeEnv, gatewayValue));
  }

  @Test
  void shouldBrokerOverrideEmbeddedGatewayConfiguration() {
    // given
    final String foreseeEnv = "IS_CONFIGURED";
    final String brokerValue = "BROKER";
    final String gatewayValue = "GATEWAY";

    final ZeebeClusterBuilder builder =
        new ZeebeClusterBuilder()
            .withBrokerConfig(broker -> broker.addEnv(foreseeEnv, brokerValue))
            .withGatewayConfig(gateway -> gateway.addEnv(foreseeEnv, gatewayValue))
            .withBrokersCount(1)
            .withGatewaysCount(0)
            .withEmbeddedGateway(true);

    // when
    final ZeebeCluster cluster = builder.build();

    // then
    assertThat(cluster.getBrokers())
        .as(
            "all brokers must have %s environment variable with %s value, e.g. must not be configured by gateway function",
            foreseeEnv, gatewayValue)
        .allSatisfy(
            (integer, zeebeBrokerNode) ->
                assertThat(zeebeBrokerNode.getEnvMap())
                    .as(
                        "Broker node: %s must not have %s environment variable",
                        zeebeBrokerNode, foreseeEnv)
                    .containsEntry(foreseeEnv, brokerValue));
    assertThat(cluster.getGateways())
        .as(
            "all gateways must not have %s environment variable with %s value, e.g. must not be configured by gateway function",
            foreseeEnv, brokerValue)
        .allSatisfy(
            (s, zeebeGatewayNode) ->
                assertThat(zeebeGatewayNode.getEnvMap())
                    .as(
                        "Gateway node: %s must have %s environment variable",
                        zeebeGatewayNode, foreseeEnv)
                    .doesNotContainEntry(foreseeEnv, gatewayValue));
  }

  @Test
  void shouldSetImageNameForGateways() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();
    final String zeebeDockerImage = "camunda/zeebe:latest";

    // when
    final DockerImageName gatewayImageName = DockerImageName.parse(zeebeDockerImage);
    builder.withGatewayImage(gatewayImageName).withGatewaysCount(1).withEmbeddedGateway(false);
    final ZeebeCluster cluster = builder.build();

    // then
    assertThat(cluster.getGateways().entrySet())
        .as("the only gateway created has the right docker image")
        .singleElement()
        .satisfies(
            gatewayEntry -> verifyZeebeHasImageName(gatewayEntry.getValue(), zeebeDockerImage));
  }

  @Test
  void shouldSetImageNameForBrokers() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();
    final String zeebeDockerImage = "camunda/zeebe:latest";

    // when
    final DockerImageName gatewayImageName = DockerImageName.parse(zeebeDockerImage);
    builder.withBrokerImage(gatewayImageName).withBrokersCount(1);
    final ZeebeCluster cluster = builder.build();

    // then
    assertThat(cluster.getBrokers().entrySet())
        .as("the only broker created has the right docker image")
        .singleElement()
        .satisfies(
            brokerEntry -> verifyZeebeHasImageName(brokerEntry.getValue(), zeebeDockerImage));
  }

  @Test
  void shouldSetImageNameForGatewaysAndBrokers() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();
    final String zeebeDockerImage = "camunda/zeebe:latest";

    // when
    final DockerImageName gatewayImageName = DockerImageName.parse(zeebeDockerImage);
    builder
        .withImage(gatewayImageName)
        .withBrokersCount(1)
        .withGatewaysCount(1)
        .withEmbeddedGateway(false);
    final ZeebeCluster cluster = builder.build();

    // then
    assertThat(cluster.getBrokers().entrySet())
        .as("the only broker created has the right docker image")
        .singleElement()
        .satisfies(
            brokerEntry -> verifyZeebeHasImageName(brokerEntry.getValue(), zeebeDockerImage));
    assertThat(cluster.getGateways().entrySet())
        .as("the only standalone gateway created has the right docker image")
        .singleElement()
        .satisfies(
            gatewayEntry -> verifyZeebeHasImageName(gatewayEntry.getValue(), zeebeDockerImage));
  }

  private Condition<ZeebeNode<? extends GenericContainer<?>>> zeebeImageHasImageName(
      final String imageName) {
    return new Condition<>(
        node -> node.getDockerImageName().equals(imageName), "Image Name Condition");
  }

  private void verifyZeebeHasImageName(
      final ZeebeNode<? extends GenericContainer<?>> zeebe, final String imageName) {
    assertThat(zeebe.getDockerImageName()).isEqualTo(imageName);
  }
}

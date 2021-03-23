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
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

final class ZeebeClusterBuilderTest {
  @Test
  void shouldThrowIllegalArgumentIfBrokersCountIsNegative() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // then
    assertThatCode(() -> builder.withBrokersCount(-1)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldThrowIllegalArgumentWhenPartitionsIsNotStrictlyPositive() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // then
    assertThatCode(() -> builder.withPartitionsCount(0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatCode(() -> builder.withPartitionsCount(-1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldThrowIllegalArgumentIfReplicationFactorIsNotStrictlyPositive() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // then
    assertThatCode(() -> builder.withReplicationFactor(0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatCode(() -> builder.withReplicationFactor(-1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldThrowNullExceptionIfNetworkIsNull() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // then
    assertThatCode(() -> builder.withNetwork(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenNameIsNull() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // then
    assertThatCode(() -> builder.withName(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenNameIsTooShort() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // then
    assertThatCode(() -> builder.withName("")).isInstanceOf(IllegalArgumentException.class);
    assertThatCode(() -> builder.withName("a")).isInstanceOf(IllegalArgumentException.class);
    assertThatCode(() -> builder.withName("aa")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldThrowIllegalStateExceptionWhenReplicationFactorIsGreaterThanBrokersCount() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // when
    builder.withBrokersCount(1).withReplicationFactor(2);

    // then
    assertThatCode(builder::build).isInstanceOf(IllegalStateException.class);
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
    assertThat(brokers).hasSize(2).containsKeys(0, 1);
    assertThat(brokers.get(0).getEnvMap())
        .contains(
            entry("ZEEBE_BROKER_CLUSTER_NODEID", "0"),
            entry("ZEEBE_BROKER_CLUSTER_CLUSTERSIZE", "2"));
    assertThat(brokers.get(1).getEnvMap())
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
    assertThat(cluster.getReplicationFactor()).isZero();
    assertThat(cluster.getPartitionsCount()).isZero();
  }

  @Test
  void shouldResetPartitionsAndReplicationFactorToOneIfBrokersCountGoesFromZeroToPositive() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // when
    builder.withBrokersCount(0).withBrokersCount(3);
    final ZeebeCluster cluster = builder.build();

    // then
    assertThat(cluster.getReplicationFactor()).isEqualTo(1);
    assertThat(cluster.getPartitionsCount()).isEqualTo(1);
  }

  @Test
  void shouldNotModifyPartitionsCountOrReplicationFactoryWhenSettingBrokersCount() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // when
    builder.withPartitionsCount(4).withReplicationFactor(1).withBrokersCount(3);
    final ZeebeCluster cluster = builder.build();

    // then
    assertThat(cluster.getReplicationFactor()).isEqualTo(1);
    assertThat(cluster.getPartitionsCount()).isEqualTo(4);
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
    assertThat(cluster.getPartitionsCount()).isEqualTo(2);
    assertThat(brokers.get(0).getEnvMap())
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
    assertThat(cluster.getReplicationFactor()).isEqualTo(2);
    assertThat(brokers.get(0).getEnvMap())
        .containsEntry("ZEEBE_BROKER_CLUSTER_REPLICATIONFACTOR", "2");
    assertThat(brokers.get(1).getEnvMap())
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
    assertThat(cluster.getBrokers()).hasSize(2);
    assertThat(cluster.getGateways()).hasSize(2);

    final Set<String> internalHosts = new HashSet<>();
    cluster.getBrokers().values().stream()
        .map(ZeebeNode::getInternalHost)
        .forEach(internalHosts::add);
    cluster.getGateways().values().stream()
        .map(ZeebeNode::getInternalHost)
        .forEach(internalHosts::add);
    assertThat(internalHosts).hasSize(4).doesNotContainNull();
  }

  @Test
  void shouldAssignDifferentClusterHostsToAllNodes() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // when
    builder.withEmbeddedGateway(false).withBrokersCount(2).withGatewaysCount(2);
    final ZeebeCluster cluster = builder.build();

    // then
    assertThat(cluster.getBrokers()).hasSize(2);
    assertThat(cluster.getGateways()).hasSize(2);

    final Set<String> advertisedHosts = new HashSet<>();
    cluster.getBrokers().values().stream()
        .map(Container::getEnvMap)
        .map(env -> env.get("ZEEBE_BROKER_NETWORK_ADVERTISEDHOST"))
        .forEach(advertisedHosts::add);
    cluster.getGateways().values().stream()
        .map(Container::getEnvMap)
        .map(env -> env.get("ZEEBE_GATEWAY_CLUSTER_HOST"))
        .forEach(advertisedHosts::add);
    assertThat(advertisedHosts).hasSize(4).doesNotContainNull();
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
    assertThat(cluster.getBrokers()).hasSize(2);
    assertThat(cluster.getGateways()).hasSize(2);

    cluster
        .getBrokers()
        .values()
        .forEach(
            b ->
                assertThat(b.getEnvMap())
                    .containsEntry("ZEEBE_BROKER_CLUSTER_CLUSTERNAME", "test-cluster"));
    cluster
        .getGateways()
        .values()
        .forEach(
            g ->
                assertThat(g.getEnvMap())
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
        .forEach(b -> assertThat(b.self().getNetwork()).isEqualTo(network));
    cluster
        .getGateways()
        .values()
        .forEach(g -> assertThat(g.self().getNetwork()).isEqualTo(network));
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
      assertThat(gateway.getEnvMap()).containsEntry("ZEEBE_GATEWAY_CLUSTER_MEMBERID", memberId);
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

    assertThat(gateway.getEnvMap()).doesNotContainKey("ZEEBE_GATEWAY_CLUSTER_CONTACTPOINT");
  }

  @Test
  void shouldConfigureEmbeddedGateway() {
    // given
    final ZeebeClusterBuilder builder = new ZeebeClusterBuilder();

    // when
    builder.withEmbeddedGateway(true).withBrokersCount(1);
    final ZeebeCluster cluster = builder.build();

    // then
    assertThat(cluster.getGateways()).hasSize(1);
    assertThat(cluster.getGateways().get("0")).isInstanceOf(ZeebeBrokerNode.class);
    assertThat(cluster.getGateways().get("0").getEnvMap())
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
    assertThat(cluster.getGateways()).isEmpty();
    assertThat(cluster.getBrokers().get(0).getEnvMap())
        .doesNotContainEntry("ZEEBE_BROKER_GATEWAY_ENABLE", "true");
  }
}

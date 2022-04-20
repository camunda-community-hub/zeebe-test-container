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

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.Topology;
import io.zeebe.containers.ZeebeGatewayNode;
import io.zeebe.containers.util.TopologyAssert;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class ZeebeClusterTest {
  private ZeebeCluster cluster;

  @AfterEach
  void afterEach() {
    Optional.ofNullable(cluster).ifPresent(ZeebeCluster::stop);
  }

  @Test
  void shouldStartSingleNodeCluster() {
    // given
    cluster =
        ZeebeCluster.builder()
            .withEmbeddedGateway(true)
            .withReplicationFactor(1)
            .withPartitionsCount(1)
            .withBrokersCount(1)
            .build();

    // when
    cluster.start();

    // then
    final ZeebeClient client = cluster.newClientBuilder().build();
    final Topology topology = client.newTopologyRequest().send().join();
    assertThat(topology.getPartitionsCount())
        .as("there is exactly one partition as configured")
        .isEqualTo(1);
    assertThat(topology.getReplicationFactor())
        .as("there is a replication factor of 1 as configured")
        .isEqualTo(1);
    TopologyAssert.assertThat(topology)
        .as("the topology is complete for a one broker, one partition cluster")
        .hasBrokersCount(1)
        .isComplete(1, 1, 1);
  }

  @Test
  void shouldStartClusterWithEmbeddedGateways() {
    // given
    cluster =
        ZeebeCluster.builder()
            .withEmbeddedGateway(true)
            .withReplicationFactor(2)
            .withPartitionsCount(2)
            .withBrokersCount(2)
            .build();

    // when
    cluster.start();

    // then
    for (final ZeebeGatewayNode<?> gateway : cluster.getGateways().values()) {
      try (final ZeebeClient client =
          ZeebeClient.newClientBuilder()
              .usePlaintext()
              .gatewayAddress(gateway.getExternalGatewayAddress())
              .build()) {
        final Topology topology = client.newTopologyRequest().send().join();
        assertThat(topology.getReplicationFactor())
            .as("there is replication factor of 2 as configured")
            .isEqualTo(2);
        assertThat(topology.getPartitionsCount())
            .as("there are exactly two partitions as configured")
            .isEqualTo(2);
        TopologyAssert.assertThat(topology)
            .as("the topology is complete with 2 partitions and 2 brokers")
            .hasBrokersCount(2)
            .isComplete(2, 2, 2);
      }
    }
  }

  @Test
  void shouldStartClusterWithStandaloneGateway() {
    // given
    cluster =
        ZeebeCluster.builder()
            .withEmbeddedGateway(false)
            .withReplicationFactor(1)
            .withPartitionsCount(1)
            .withBrokersCount(1)
            .withGatewaysCount(1)
            .build();

    // when
    cluster.start();

    // then
    try (final ZeebeClient client = cluster.newClientBuilder().build()) {
      final Topology topology = client.newTopologyRequest().send().join();
      assertThat(topology.getPartitionsCount())
          .as("there is exactly one partition as configured")
          .isEqualTo(1);
      assertThat(topology.getReplicationFactor())
          .as("there is a replication factor of 1 as configured")
          .isEqualTo(1);
      TopologyAssert.assertThat(topology)
          .as("the topology is complete for a one broker, one partition cluster")
          .hasBrokersCount(1)
          .isComplete(1, 1, 1);
    }
  }

  @Test
  void shouldStartClusterWithMixedGateways() {
    // given
    cluster =
        ZeebeCluster.builder()
            .withEmbeddedGateway(true)
            .withReplicationFactor(1)
            .withPartitionsCount(1)
            .withBrokersCount(1)
            .withGatewaysCount(1)
            .build();

    // when
    cluster.start();

    // then
    for (final ZeebeGatewayNode<?> gateway : cluster.getGateways().values()) {
      try (final ZeebeClient client =
          ZeebeClient.newClientBuilder()
              .usePlaintext()
              .gatewayAddress(gateway.getExternalGatewayAddress())
              .build()) {
        final Topology topology = client.newTopologyRequest().send().join();
        assertThat(topology.getPartitionsCount())
            .as("there is exactly one partition as configured")
            .isEqualTo(1);
        assertThat(topology.getReplicationFactor())
            .as("there is a replication factor of 1 as configured")
            .isEqualTo(1);
        TopologyAssert.assertThat(topology)
            .as("the topology is complete for a one broker, one partition cluster")
            .isComplete(1, 1, 1)
            .hasBrokersCount(1);
      }
    }
  }
}

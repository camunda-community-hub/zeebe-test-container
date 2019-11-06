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

import static org.assertj.core.api.Assertions.assertThat;

import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.response.BrokerInfo;
import io.zeebe.client.api.response.Topology;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

public class ZeebeTest extends CompatibilityTestCase {
  private static final String CLUSTER_NAME = "zeebe";

  @Test
  public void shouldStartConnectedGatewayAndBroker() {
    // given
    final ZeebeBrokerContainer broker = newBroker();
    final ZeebeStandaloneGatewayContainer gateway = newGateway().withNetwork(broker.getNetwork());

    // when
    broker.withEmbeddedGateway(false).withHost("zeebe-0");
    gateway.withContactPoint(broker.getInternalAddress(ZeebePort.INTERNAL_API));
    Stream.of(gateway, broker).parallel().forEach(GenericContainer::start);

    // then
    final ZeebeClient client = newClient(gateway);
    final Topology topology = tryGetTopology(client, 1, 1);
    final List<BrokerInfo> brokers = topology.getBrokers();
    assertThat(brokers).hasSize(1);

    final BrokerInfo brokerInfo = brokers.get(0);
    assertThat(brokerInfo.getHost()).isEqualTo("zeebe-0");
    assertThat(brokerInfo.getNodeId()).isEqualTo(0);
    assertThat(brokerInfo.getPort()).isEqualTo(ZeebePort.COMMAND_API.getPort());
    assertThat(brokerInfo.getAddress()).isEqualTo(broker.getInternalAddress(ZeebePort.COMMAND_API));
    assertThat(topology.getClusterSize()).isEqualTo(1);

    Stream.of(broker, gateway).parallel().forEach(GenericContainer::stop);
  }

  @Test
  public void shouldStartClusterAndGateway() {
    final ZeebeBrokerContainer zeebe0 = newClusterBroker(0, 3);
    final ZeebeBrokerContainer zeebe1 = newClusterBroker(1, 3).withNetwork(zeebe0.getNetwork());
    final ZeebeBrokerContainer zeebe2 = newClusterBroker(2, 3).withNetwork(zeebe0.getNetwork());
    final ZeebeStandaloneGatewayContainer gateway = newGatewayForBroker(zeebe0);
    final Collection<String> contactPoints =
        Stream.of(zeebe0, zeebe1, zeebe2)
            .map(b -> b.getInternalAddress(ZeebePort.INTERNAL_API))
            .collect(Collectors.toList());

    // set contact points for all
    zeebe0.withContactPoints(contactPoints);
    zeebe1.withContactPoints(contactPoints);
    zeebe2.withContactPoints(contactPoints);

    // start all brokers
    Stream.of(gateway, zeebe0, zeebe1, zeebe2).parallel().forEach(GenericContainer::start);

    // Verify topology
    final ZeebeClient client = newClient(gateway);
    final Topology topology = tryGetTopology(client, 3, 3);
    final List<BrokerInfo> brokers = topology.getBrokers();
    assertThat(brokers).hasSize(3);

    // stop all brokers
    Stream.of(zeebe0, zeebe1, zeebe2, gateway).parallel().forEach(GenericContainer::stop);
  }

  private ZeebeClient newClient(final ZeebeStandaloneGatewayContainer gateway) {
    return ZeebeClient.newClientBuilder()
        .brokerContactPoint(gateway.getExternalAddress(ZeebePort.GATEWAY))
        .usePlaintext()
        .build();
  }

  private ZeebeStandaloneGatewayContainer newGatewayForBroker(final ZeebeBrokerContainer broker) {
    final ZeebeStandaloneGatewayContainer container = newGateway().withNetwork(broker.getNetwork());
    container
        .withClusterHost("gateway")
        .withClusterMemberId("gateway")
        .withClusterName(CLUSTER_NAME)
        .withContactPoint(broker.getInternalAddress(ZeebePort.INTERNAL_API));

    return container;
  }

  private ZeebeBrokerContainer newClusterBroker(final int nodeId, final int clusterSize) {
    final ZeebeBrokerContainer container = newBroker();
    container
        .withEmbeddedGateway(false)
        .withPartitionCount(clusterSize)
        .withReplicationFactor(clusterSize)
        .withNodeId(nodeId)
        .withHost("zeebe-" + nodeId)
        .withClusterName(CLUSTER_NAME)
        .withClusterSize(clusterSize);

    return container;
  }

  private ZeebeBrokerContainer newBroker() {
    return new ZeebeBrokerContainer(version);
  }

  private ZeebeStandaloneGatewayContainer newGateway() {
    return new ZeebeStandaloneGatewayContainer(version);
  }
}

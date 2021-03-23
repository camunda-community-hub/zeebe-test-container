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
package io.zeebe.containers.examples.cluster;

import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.response.BrokerInfo;
import io.zeebe.client.api.response.Topology;
import io.zeebe.containers.cluster.ZeebeCluster;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * This example show cases how to create a simple test against a single node broker with embedded
 * gateway.
 */
class ZeebeClusterSingleNodeExampleTest {
  private final ZeebeCluster cluster =
      ZeebeCluster.builder()
          .withGatewaysCount(0)
          .withBrokersCount(1)
          .withPartitionsCount(1)
          .withReplicationFactor(1)
          .withEmbeddedGateway(true)
          .build();

  @Test
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  void shouldConnectToZeebe() {
    // given
    cluster.start();

    // when
    final Topology topology;
    try (final ZeebeClient client = cluster.newClientBuilder().build()) {
      topology = client.newTopologyRequest().send().join(5, TimeUnit.SECONDS);
    }

    // then
    final List<BrokerInfo> brokers = topology.getBrokers();
    Assertions.assertThat(topology.getClusterSize()).isEqualTo(1);
    Assertions.assertThat(brokers)
        .hasSize(1)
        .extracting(BrokerInfo::getAddress)
        .containsExactlyInAnyOrder(cluster.getBrokers().get(0).getInternalCommandAddress());
  }
}

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

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.BrokerInfo;
import io.camunda.zeebe.client.api.response.Topology;
import io.zeebe.containers.cluster.ZeebeCluster;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Showcases how you can create a test with a cluster of two brokers and one standalone gateway.
 * Configuration is kept to minimum, as the goal here is only to showcase how to connect the
 * different nodes together.
 */
class ZeebeClusterWithGatewayExampleTest {
  private final ZeebeCluster cluster =
      ZeebeCluster.builder()
          .withEmbeddedGateway(false)
          .withGatewaysCount(1)
          .withBrokersCount(1)
          .withPartitionsCount(1)
          .withReplicationFactor(1)
          .build();

  @AfterEach
  void tearDown() {
    cluster.stop();
  }

  @Test
  @Timeout(value = 15, unit = TimeUnit.MINUTES)
  void shouldStartCluster() {
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

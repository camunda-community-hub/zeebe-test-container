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
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rnorth.ducttape.timeouts.Timeouts;

public class ZeebeBrokerContainerTest extends CompatibilityTestCase {
  private ZeebeBrokerContainer container;

  @Before
  public void setUp() {
    container = new ZeebeBrokerContainer(version);
  }

  @After
  public void tearDown() {
    container.stop();
    container = null;
  }

  @Test
  public void shouldStartWithEmbeddedGateway() {
    // given
    final int partitionsCount = 3;
    container
        .withHost("zeebe-0")
        .withNodeId(0)
        .withEmbeddedGateway(true)
        .withPartitionCount(partitionsCount)
        .withReplicationFactor(1);
    Timeouts.doWithTimeout(30, TimeUnit.SECONDS, container::start);

    // when
    final ZeebeClient client =
        ZeebeClient.newClientBuilder()
            .usePlaintext()
            .brokerContactPoint(container.getExternalAddress(ZeebePort.GATEWAY))
            .build();
    // we need to wait until the broker's topology is stable in order to really test it, as the
    final Topology topology = tryGetTopology(client, 1, partitionsCount);
    final List<BrokerInfo> brokers = topology.getBrokers();
    final BrokerInfo brokerInfo = brokers.get(0);

    // then
    assertThat(topology.getClusterSize()).isEqualTo(1);
    assertThat(topology.getReplicationFactor()).isEqualTo(1);
    assertThat(topology.getPartitionsCount()).isEqualTo(partitionsCount);
    assertThat(brokers).hasSize(1);
    assertThat(brokerInfo.getHost()).isEqualTo("zeebe-0");
    assertThat(brokerInfo.getNodeId()).isEqualTo(0);
    assertThat(brokerInfo.getPort()).isEqualTo(ZeebePort.COMMAND_API.getPort());
    assertThat(brokerInfo.getAddress())
        .isEqualTo(container.getInternalAddress(ZeebePort.COMMAND_API));
    assertThat(brokerInfo.getPartitions()).hasSize(partitionsCount);
  }
}

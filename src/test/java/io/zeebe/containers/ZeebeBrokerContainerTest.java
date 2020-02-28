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
import io.zeebe.containers.api.ZeebeBrokerContainer;
import io.zeebe.containers.api.ZeebeTestContainerFactory;
import io.zeebe.containers.impl.ZeebePort;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.rnorth.ducttape.timeouts.Timeouts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

@Timeout(value = 15, unit = TimeUnit.MINUTES)
class ZeebeBrokerContainerTest {
  private static final Pattern DOUBLE_NEWLINE = Pattern.compile("\n\n");
  private static final Logger LOGGER = LoggerFactory.getLogger("ZeebeBrokerContainerTest");

  private ZeebeBrokerContainer container;
  private Network network;

  @AfterEach
  void tearDown() {
    if (container != null) {
      appendContainerLogsToTestLog();

      container.stop();
      container = null;
    }

    if (network != null) {
      network.close();
      network = null;
    }
  }

  private void appendContainerLogsToTestLog() {
    if (LOGGER.isErrorEnabled()) {
      LOGGER.error(
          String.format(
              "%n===============================================%nContainer logs%n===============================================%n%s",
              container.getLogs().replaceAll(DOUBLE_NEWLINE.pattern(), "\n")));
    }
  }

  @BeforeEach
  void setUp() {
    network = Network.newNetwork();
  }

  @ParameterizedTest
  @EnumSource(SupportedVersion.class)
  void shouldStartWithEmbeddedGateway(final SupportedVersion version) {
    // given
    final int partitionsCount = 3;

    final ZeebeTestContainerFactory containerFacory =
        ZeebeTestContainer.getFactory(version.semanticVersion());

    container =
        containerFacory
            .createBrokerContainer()
            .withBrokerHost("zeebe-0")
            .withBrokerNodeId(0)
            .withEmbeddedGateway(true)
            .withPartitionCount(partitionsCount)
            .withReplicationFactor(1)
            .withNetwork(network)
            .withConfiguration(
                getClass()
                    .getClassLoader()
                    .getResourceAsStream(getGatewayConfigurationResourceName(version)));

    Timeouts.doWithTimeout(30, TimeUnit.SECONDS, container::start);

    // when
    final ZeebeClient client =
        ZeebeClient.newClientBuilder()
            .usePlaintext()
            .brokerContactPoint(container.getExternalAddress(ZeebePort.GATEWAY))
            .build();
    // we need to wait until the broker's topology is stable in order to really test it, as the
    final Topology topology = Awaitables.awaitTopology(client, 1, partitionsCount);
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

  private String getGatewayConfigurationResourceName(SupportedVersion version) {
    switch (version) {
      case ZEEBE_0_20_1:
      case ZEEBE_0_21_1:
      case ZEEBE_0_22_1:
        return "zeebe.cfg.toml";
      case ZEEBE_0_23_0_alpha2:
        return "zeebe.cfg.yaml";
      default:
        throw new IllegalArgumentException("Unhandled case: " + version);
    }
  }

  @ParameterizedTest
  @EnumSource(SupportedVersion.class)
  void shouldNotOverwritePort(SupportedVersion version) {
    // given
    final ZeebeTestContainerFactory containerFacory =
        ZeebeTestContainer.getFactory(version.semanticVersion());
    container =
        containerFacory
            .createBrokerContainer()
            .withNetwork(network)
            .withEmbeddedGateway(true)
            .withExposedPorts(5701);

    // when
    container.start();

    // then
    assertThat(container.getMappedPort(5701)).isGreaterThan(0);
    Arrays.stream(ZeebePort.values())
        .map(ZeebePort::getPort)
        .forEach(port -> assertThat(container.getMappedPort(port)).isGreaterThan(0));
  }
}

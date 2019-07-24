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
import io.zeebe.containers.broker.BrokerContainer;
import io.zeebe.containers.gateway.GatewayContainer;
import java.util.stream.Stream;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

public class ZeebeTest {
  @Test
  public void shouldStartConnectedGatewayAndBroker() {
    // given
    final BrokerContainer broker = new BrokerContainer();
    final GatewayContainer gateway = new GatewayContainer().withNetwork(broker.getNetwork());

    // when
    broker.getEnvironment().withEmbeddedGateway(false).withHost("zeebe-0").withClusterName("zeebe");
    gateway
        .getEnvironment()
        .withContactPoint(broker.getInternalAddress(ZeebePort.INTERNAL_API))
        .withClusterName("zeebe");
    Stream.of(gateway, broker).parallel().forEach(GenericContainer::start);

    // then
    final ZeebeClient client =
        ZeebeClient.newClientBuilder()
            .brokerContactPoint(gateway.getExternalAddress(ZeebePort.GATEWAY))
            .build();
    assertThat(client.newTopologyRequest().send().join().getClusterSize()).isEqualTo(1);

    Stream.of(broker, gateway).parallel().forEach(GenericContainer::stop);
  }
}

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
package io.zeebe.containers.broker;

import io.zeebe.containers.ZeebeContainer;
import io.zeebe.containers.ZeebePort;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.Base58;

public class ZeebeBrokerContainer extends ZeebeContainer<ZeebeBrokerEnvironment, ZeebeBrokerContainer> {
  private static final String DEFAULT_CLUSTER_NAME = "zeebe";

  public int getPort(ZeebePort port) {
    return port.getPort() + (environment.getPortOffset() * 10);
  }

  @Override
  protected ZeebeBrokerEnvironment newDefaultEnvironment() {
    final String host = "zeebe-broker-" + Base58.randomString(6);
    return new ZeebeBrokerEnvironment()
        .withHost(host)
        .withPartitionCount(1)
        .withReplicationFactor(1)
        .withEmbeddedGateway(true)
        .withDebug(false)
        .withClusterName(DEFAULT_CLUSTER_NAME)
        .withClusterSize(1)
        .withContactPoints(Collections.emptyList())
        .withNodeId(0);
  }

  @Override
  protected Map<ZeebePort, Integer> getPorts() {
    final Set<ZeebePort> exposedPorts = EnumSet.allOf(ZeebePort.class);

    if (!environment.shouldEmbedGateway()) {
      exposedPorts.remove(ZeebePort.GATEWAY);
    }

    return exposedPorts.stream().collect(Collectors.toMap(Function.identity(), this::getPort));
  }

  @Override
  protected String getInternalHost() {
    return environment.getHost();
  }

  @Override
  protected void applyDefaultConfiguration() {
    setWaitStrategy(new LogMessageWaitStrategy().withRegEx(".*Broker is ready.*"));
    super.applyDefaultConfiguration();
  }
}

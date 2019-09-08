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
package io.zeebe.containers.gateway;

import io.zeebe.containers.ZeebeContainer;
import io.zeebe.containers.ZeebePort;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.utility.Base58;

public class ZeebeGatewayContainer extends ZeebeContainer<ZeebeGatewayEnvironment, ZeebeGatewayContainer> {
  private static final String DEFAULT_CLUSTER_NAME = "zeebe";

  @Override
  protected int getPort(ZeebePort port) {
    return port.getPort();
  }

  @Override
  protected ZeebeGatewayEnvironment newDefaultEnvironment() {
    final String host = "zeebe-gateway-" + Base58.randomString(6);
    return new ZeebeGatewayEnvironment()
        .withHost("0.0.0.0")
        .withPort(ZeebePort.GATEWAY.getPort())
        .withClusterName(DEFAULT_CLUSTER_NAME)
        .withClusterMemberId("zeebe-gateway-0")
        .withClusterPort(ZeebePort.INTERNAL_API.getPort())
        .withClusterHost(host);
  }

  @Override
  protected Map<ZeebePort, Integer> getPorts() {
    final Set<ZeebePort> exposedPorts =
        EnumSet.of(ZeebePort.GATEWAY, ZeebePort.INTERNAL_API, ZeebePort.MONITORING_API);
    return exposedPorts.stream().collect(Collectors.toMap(Function.identity(), ZeebePort::getPort));
  }

  @Override
  protected String getInternalHost() {
    return environment.getClusterHost();
  }

  @Override
  protected void applyDefaultConfiguration() {
    setWaitStrategy(new HostPortWaitStrategy());
    withEnv(ZeebeGatewayEnvVar.STANDALONE.getVariableName(), "true");
    super.applyDefaultConfiguration();
  }

  @Override
  public Set<Integer> getLivenessCheckPortNumbers() {
    final Set<Integer> ports = new HashSet<>();
    ports.add(getPort(ZeebePort.GATEWAY));
    return ports;
  }
}

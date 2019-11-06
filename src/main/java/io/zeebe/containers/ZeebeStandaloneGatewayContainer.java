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

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.utility.Base58;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
public class ZeebeStandaloneGatewayContainer
    extends GenericContainer<ZeebeStandaloneGatewayContainer>
    implements ZeebeContainer<ZeebeStandaloneGatewayContainer>,
        ZeebeGatewayContainer<ZeebeStandaloneGatewayContainer>,
        ZeebeNetworkable {

  protected static final String DEFAULT_CLUSTER_MEMBER_ID = "zeebe-gateway-0";
  protected static final String DEFAULT_HOST = "0.0.0.0";

  protected String internalHost;
  protected boolean monitoringEnabled;

  public ZeebeStandaloneGatewayContainer() {
    this(ZeebeDefaults.getInstance().getDefaultVersion());
  }

  public ZeebeStandaloneGatewayContainer(final String version) {
    this(ZeebeDefaults.getInstance().getDefaultImage(), version);
  }

  public ZeebeStandaloneGatewayContainer(final String image, final String version) {
    super(image + ":" + version);
    applyDefaultConfiguration();
  }

  public void applyDefaultConfiguration() {
    final String defaultInternalHost = "zeebe-gateway-" + Base58.randomString(6);

    withHost(DEFAULT_HOST)
        .withPort(ZeebePort.GATEWAY.getPort())
        .withClusterName(ZeebeDefaults.getInstance().getDefaultClusterName())
        .withClusterMemberId(DEFAULT_CLUSTER_MEMBER_ID)
        .withClusterPort(ZeebePort.INTERNAL_API.getPort())
        .withClusterHost(defaultInternalHost);

    setWaitStrategy(new HostPortWaitStrategy());
    withEnv(ZeebeStandaloneGatewayEnvironment.STANDALONE, true);
    withNetwork(Network.newNetwork());
  }

  @Override
  protected void configure() {
    final String name = getInternalHost() + "-" + Base58.randomString(6);
    final Set<ZeebePort> exposedPorts = EnumSet.of(ZeebePort.GATEWAY);
    if (monitoringEnabled) {
      exposedPorts.add(ZeebePort.MONITORING_API);
    }

    super.configure();
    withExposedPorts(exposedPorts.stream().map(ZeebePort::getPort).toArray(Integer[]::new));
    withNetworkAliases(getInternalHost());
    withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withName(name));
  }

  @Override
  public Set<Integer> getLivenessCheckPortNumbers() {
    final Set<Integer> ports = new HashSet<>();
    ports.add(getMappedPort(ZeebePort.GATEWAY.getPort()));
    return ports;
  }

  @Override
  public String getInternalHost() {
    return internalHost;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    if (!super.equals(o)) {
      return false;
    }

    final ZeebeStandaloneGatewayContainer that = (ZeebeStandaloneGatewayContainer) o;
    return monitoringEnabled == that.monitoringEnabled
        && Objects.equals(getInternalHost(), that.getInternalHost());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getInternalHost(), monitoringEnabled);
  }

  public ZeebeStandaloneGatewayContainer withClusterName(final String clusterName) {
    return withEnv(ZeebeStandaloneGatewayEnvironment.CLUSTER_NAME, clusterName);
  }

  public ZeebeStandaloneGatewayContainer withClusterMemberId(final String clusterMemberId) {
    return withEnv(ZeebeStandaloneGatewayEnvironment.CLUSTER_MEMBER_ID, clusterMemberId);
  }

  public ZeebeStandaloneGatewayContainer withClusterHost(final String clusterHost) {
    internalHost = clusterHost;
    return withEnv(ZeebeStandaloneGatewayEnvironment.CLUSTER_HOST, clusterHost);
  }

  public ZeebeStandaloneGatewayContainer withClusterPort(final int clusterPort) {
    return withEnv(ZeebeStandaloneGatewayEnvironment.CLUSTER_PORT, clusterPort);
  }

  @Override
  public ZeebeStandaloneGatewayContainer withMonitoringEnabled(final boolean monitoringEnabled) {
    this.monitoringEnabled = monitoringEnabled;
    return ZeebeGatewayContainer.super.withMonitoringEnabled(monitoringEnabled);
  }
}

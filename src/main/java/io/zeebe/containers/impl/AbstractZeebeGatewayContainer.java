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
package io.zeebe.containers.impl;

import static io.zeebe.containers.impl.ZeebeGlobalDefaults.globalDefaults;

import de.skuzzle.semantic.Version;
import io.zeebe.containers.api.ZeebeEnvironment;
import io.zeebe.containers.api.ZeebeGatewayContainer;
import io.zeebe.containers.api.ZeebeGatewayEnvironment;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.utility.Base58;

public abstract class AbstractZeebeGatewayContainer<
        SELF extends AbstractZeebeGatewayContainer<SELF>>
    extends AbstractZeebeContainer<SELF> implements ZeebeGatewayContainer<SELF> {

  protected static final String DEFAULT_CLUSTER_MEMBER_ID = "zeebe-gateway-0";
  protected static final String DEFAULT_HOST = "0.0.0.0";

  public AbstractZeebeGatewayContainer(final Version version) {
    this(globalDefaults().getDefaultImage() + ":" + version.toString());
  }

  public AbstractZeebeGatewayContainer(final String dockerImageNameWithTag) {
    super(dockerImageNameWithTag);
    applyDefaultConfiguration();
  }

  public void applyDefaultConfiguration() {
    final String defaultInternalHost = "zeebe-gateway-" + Base58.randomString(6);

    withGatewayHost(DEFAULT_HOST)
        .withGatewayPort(ZeebePort.GATEWAY.getPort())
        .withClusterName(globalDefaults().getDefaultClusterName())
        .withClusterMemberId(DEFAULT_CLUSTER_MEMBER_ID)
        .withGatewayClusterPort(ZeebePort.INTERNAL_API.getPort())
        .withGatewayClusterHost(defaultInternalHost);

    setWaitStrategy(new HostPortWaitStrategy());
    withEnv(getZeebeGatewayEnvironment().getStandaloneGateway(), true);
    withNetwork(Network.newNetwork());
  }

  protected abstract ZeebeGatewayEnvironment getZeebeGatewayEnvironment();

  protected ZeebeEnvironment getZeebeEnvironment() {
    return getZeebeGatewayEnvironment();
  }

  @Override
  protected void configure() {
    final String name = getInternalHost() + "-" + Base58.randomString(6);
    final Set<ZeebePort> exposedPorts = EnumSet.of(ZeebePort.GATEWAY);

    super.configure();
    exposedPorts.stream().map(ZeebePort::getPort).forEach(this::addExposedPort);
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
  public SELF withGatewayHost(final String host) {
    return withEnv(getZeebeGatewayEnvironment().getGatewayHost(), host);
  }

  @Override
  public SELF withGatewayPort(final int port) {
    return withEnv(getZeebeGatewayEnvironment().getGatewayPort(), port);
  }

  public SELF withGatewayContactPoint(final String contactPoint) {
    return withEnv(getZeebeGatewayEnvironment().getGatewayContactPoint(), contactPoint);
  }

  @Override
  public SELF withClusterName(final String clusterName) {
    return withEnv(getZeebeGatewayEnvironment().getGatewayClusterName(), clusterName);
  }

  @Override
  public SELF withClusterMemberId(final String clusterMemberId) {
    return withEnv(getZeebeGatewayEnvironment().getGatewayClusterMemberId(), clusterMemberId);
  }

  @Override
  public SELF withGatewayClusterHost(final String host) {
    return withEnv(getZeebeGatewayEnvironment().getGatewayClusterHost(), host);
  }

  @Override
  public SELF withGatewayClusterPort(final int port) {
    return withEnv(getZeebeGatewayEnvironment().getGatewayClusterPort(), port);
  }

  @Override
  public SELF withGatewayKeepAliveInterval(final Duration keepAliveInterval) {
    return withEnv(getZeebeGatewayEnvironment().getGatewayKeepAliveInterval(), keepAliveInterval);
  }

  @Override
  public SELF withGatewayRequestTimeout(final Duration requestTimeout) {
    return withEnv(getZeebeGatewayEnvironment().getGatewayRequestTimeout(), requestTimeout);
  }

  @Override
  public SELF withGatewayManagementThreadCount(final int managementThreadCount) {
    return withEnv(
        getZeebeGatewayEnvironment().getGatewayManagementThreadCount(), managementThreadCount);
  }
}

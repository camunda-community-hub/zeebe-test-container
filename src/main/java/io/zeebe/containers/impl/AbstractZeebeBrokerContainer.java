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
import io.zeebe.containers.api.ZeebeBrokerContainer;
import io.zeebe.containers.api.ZeebeBrokerEnvironment;
import io.zeebe.containers.api.ZeebeEnvironment;
import java.time.Duration;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.Base58;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
public abstract class AbstractZeebeBrokerContainer<SELF extends AbstractZeebeBrokerContainer<SELF>>
    extends AbstractZeebeContainer<SELF> implements ZeebeBrokerContainer<SELF> {

  protected String host;
  protected int portOffset;
  protected boolean embedGateway;
  protected Version version;

  public AbstractZeebeBrokerContainer(final Version version) {
    this(globalDefaults().getDefaultImage() + ":" + version.toString());
  }

  public AbstractZeebeBrokerContainer(final String dockerImageNameWithTag) {
    super(dockerImageNameWithTag);
    this.version = version;
    applyDefaultConfiguration();
  }

  protected abstract ZeebeBrokerEnvironment getZeebeBrokerEnvironment();

  protected ZeebeEnvironment getZeebeEnvironment() {
    return getZeebeBrokerEnvironment();
  }

  @Override
  public String getInternalHost() {
    return host;
  }

  @Override
  protected void configure() {
    final String name = getInternalHost() + "-" + Base58.randomString(6);
    final Set<ZeebePort> exposedPorts = EnumSet.allOf(ZeebePort.class);
    if (!embedGateway) {
      exposedPorts.remove(ZeebePort.GATEWAY);
    }

    super.configure();
    exposedPorts.stream().map(ZeebePort::getPort).forEach(this::addExposedPort);
    withNetworkAliases(getInternalHost());
    withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withName(name));
  }

  public void applyDefaultConfiguration() {
    final String defaultHost = "zeebe-broker-" + Base58.randomString(6);
    setWaitStrategy(new BrokerWaitStrategy());

    withBrokerHost(defaultHost)
        .withPartitionCount(1)
        .withReplicationFactor(1)
        .withEmbeddedGateway(true)
        .withDebugFlag(false)
        .withClusterName(globalDefaults().getDefaultClusterName())
        .withClusterSize(1)
        .withContactPoints(Collections.emptyList())
        .withBrokerNodeId(0);

    withNetwork(Network.newNetwork());
  }

  public String getContactPoint() {
    return getInternalAddress(ZeebePort.INTERNAL_API);
  }

  @Override
  public SELF withBrokerHost(final String host) {
    this.host = host;
    return withEnv(getZeebeBrokerEnvironment().getBrokerHost(), host);
  }

  public SELF withBrokerNodeId(final int nodeId) {
    return withEnv(getZeebeBrokerEnvironment().getBrokerNodeId(), nodeId);
  }

  public SELF withPortOffset(final int portOffset) {
    this.portOffset = portOffset;
    return withEnv(getZeebeBrokerEnvironment().getPortOffset(), portOffset);
  }

  public SELF withReplicationFactor(final int replicationFactor) {
    return withEnv(getZeebeBrokerEnvironment().getReplicationFactor(), replicationFactor);
  }

  public SELF withPartitionCount(final int partitionCount) {
    return withEnv(getZeebeBrokerEnvironment().getPartitionsCount(), partitionCount);
  }

  public SELF withClusterSize(final int clusterSize) {
    return withEnv(getZeebeBrokerEnvironment().getClusterSize(), clusterSize);
  }

  public SELF withContactPoints(final List<String> contactPoints) {
    return withEnv(getZeebeBrokerEnvironment().getContactPoints(), contactPoints);
  }

  public SELF withDebugFlag(final boolean debug) {
    return withEnv(getZeebeBrokerEnvironment().getDebugFlag(), debug);
  }

  @Override
  public SELF withGatewayHost(final String host) {
    return withEnv(getZeebeBrokerEnvironment().getGatewayHost(), host);
  }

  @Override
  public SELF withGatewayPort(final int port) {
    return withEnv(getZeebeBrokerEnvironment().getGatewayPort(), port);
  }

  @Override
  public SELF withGatewayKeepAliveInterval(final Duration keepAliveInterval) {
    return withEnv(getZeebeBrokerEnvironment().getGatewayKeepAliveInterval(), keepAliveInterval);
  }

  @Override
  public SELF withGatewayRequestTimeout(final Duration requestTimeout) {
    return withEnv(getZeebeBrokerEnvironment().getGatewayRequestTimeout(), requestTimeout);
  }

  @Override
  public SELF withGatewayManagementThreadCount(final int managementThreadCount) {
    return withEnv(
        getZeebeBrokerEnvironment().getGatewayManagementThreadCount(), managementThreadCount);
  }

  public SELF withEmbeddedGateway(final boolean embedGateway) {
    this.embedGateway = embedGateway;
    return withEnv(getZeebeBrokerEnvironment().getEmbedGatewayFlag(), embedGateway);
  }

  @Override
  public SELF withClusterName(final String clusterName) {
    return withEnv(getZeebeBrokerEnvironment().getClusterName(), clusterName);
  }
}

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

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.Base58;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
public class ZeebeBrokerContainer extends GenericContainer<ZeebeBrokerContainer>
    implements ZeebeContainer<ZeebeBrokerContainer>,
        ZeebeGatewayContainer<ZeebeBrokerContainer>,
        ZeebeNetworkable {
  protected String host;
  protected int portOffset;
  protected boolean embedGateway;

  public ZeebeBrokerContainer() {
    this(ZeebeDefaults.getInstance().getDefaultVersion());
  }

  public ZeebeBrokerContainer(final String version) {
    this(ZeebeDefaults.getInstance().getDefaultImage(), version);
  }

  public ZeebeBrokerContainer(final String image, final String version) {
    super(image + ":" + version);
    applyDefaultConfiguration();
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
    withExposedPorts(exposedPorts.stream().map(ZeebePort::getPort).toArray(Integer[]::new));
    withNetworkAliases(getInternalHost());
    withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withName(name));
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

    final ZeebeBrokerContainer that = (ZeebeBrokerContainer) o;
    return portOffset == that.portOffset
        && embedGateway == that.embedGateway
        && Objects.equals(host, that.host);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), host, portOffset, embedGateway);
  }

  public void applyDefaultConfiguration() {
    final String defaultHost = "zeebe-broker-" + Base58.randomString(6);
    setWaitStrategy(new LogMessageWaitStrategy().withRegEx(".*Broker is ready.*"));

    withHost(defaultHost)
        .withPartitionCount(1)
        .withReplicationFactor(1)
        .withEmbeddedGateway(true)
        .withDebug(false)
        .withClusterName(ZeebeDefaults.getInstance().getDefaultClusterName())
        .withClusterSize(1)
        .withContactPoints(Collections.emptyList())
        .withNodeId(0);

    withNetwork(Network.newNetwork());
  }

  public String getContactPoint() {
    return getInternalAddress(ZeebePort.INTERNAL_API);
  }

  @Override
  public ZeebeBrokerContainer withHost(final String host) {
    this.host = host;
    return withEnv(ZeebeBrokerEnvironment.HOST, host);
  }

  public ZeebeBrokerContainer withNodeId(final int nodeId) {
    return withEnv(ZeebeBrokerEnvironment.NODE_ID, nodeId);
  }

  public ZeebeBrokerContainer withPortOffset(final int portOffset) {
    this.portOffset = portOffset;
    return withEnv(ZeebeBrokerEnvironment.PORT_OFFSET, portOffset);
  }

  public ZeebeBrokerContainer withReplicationFactor(final int replicationFactor) {
    return withEnv(ZeebeBrokerEnvironment.REPLICATION_FACTOR, replicationFactor);
  }

  public ZeebeBrokerContainer withPartitionCount(final int partitionCount) {
    return withEnv(ZeebeBrokerEnvironment.PARTITION_COUNT, partitionCount);
  }

  public ZeebeBrokerContainer withClusterSize(final int clusterSize) {
    return withEnv(ZeebeBrokerEnvironment.CLUSTER_SIZE, clusterSize);
  }

  public ZeebeBrokerContainer withClusterName(final String clusterName) {
    return withEnv(ZeebeBrokerEnvironment.CLUSTER_NAME, clusterName);
  }

  public ZeebeBrokerContainer withContactPoints(final Collection<String> contactPoints) {
    return withEnv(ZeebeBrokerEnvironment.CONTACT_POINTS, contactPoints);
  }

  public ZeebeBrokerContainer withDebug(final boolean debug) {
    return withEnv(ZeebeBrokerEnvironment.DEBUG, debug);
  }

  public ZeebeBrokerContainer withEmbeddedGateway(final boolean embedGateway) {
    this.embedGateway = embedGateway;
    return withEnv(ZeebeBrokerEnvironment.EMBED_GATEWAY, embedGateway);
  }
}

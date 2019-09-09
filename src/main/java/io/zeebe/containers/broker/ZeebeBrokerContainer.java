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

import io.zeebe.containers.ZeebeConfigurable;
import io.zeebe.containers.ZeebeDefaults;
import io.zeebe.containers.ZeebeNetworkable;
import io.zeebe.containers.ZeebePort;
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
    implements ZeebeConfigurable<ZeebeBrokerContainer>, ZeebeNetworkable {
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

  public ZeebeBrokerContainer withHost(final String host) {
    this.host = host;
    return withEnv(ZeebeBrokerEnvironmentVariable.HOST, host);
  }

  public ZeebeBrokerContainer withNodeId(final int nodeId) {
    return withEnv(ZeebeBrokerEnvironmentVariable.NODE_ID, nodeId);
  }

  public ZeebeBrokerContainer withPortOffset(final int portOffset) {
    this.portOffset = portOffset;
    return withEnv(ZeebeBrokerEnvironmentVariable.PORT_OFFSET, portOffset);
  }

  public ZeebeBrokerContainer withReplicationFactor(final int replicationFactor) {
    return withEnv(ZeebeBrokerEnvironmentVariable.REPLICATION_FACTOR, replicationFactor);
  }

  public ZeebeBrokerContainer withPartitionCount(final int partitionCount) {
    return withEnv(ZeebeBrokerEnvironmentVariable.PARTITION_COUNT, partitionCount);
  }

  public ZeebeBrokerContainer withClusterSize(final int clusterSize) {
    return withEnv(ZeebeBrokerEnvironmentVariable.CLUSTER_SIZE, clusterSize);
  }

  public ZeebeBrokerContainer withClusterName(final String clusterName) {
    return withEnv(ZeebeBrokerEnvironmentVariable.CLUSTER_NAME, clusterName);
  }

  public ZeebeBrokerContainer withContactPoints(final Collection<String> contactPoints) {
    return withEnv(ZeebeBrokerEnvironmentVariable.CONTACT_POINTS, contactPoints);
  }

  public ZeebeBrokerContainer withDebug(final boolean debug) {
    return withEnv(ZeebeBrokerEnvironmentVariable.DEBUG, debug);
  }

  public ZeebeBrokerContainer withEmbeddedGateway(final boolean embedGateway) {
    this.embedGateway = embedGateway;
    return withEnv(ZeebeBrokerEnvironmentVariable.EMBED_GATEWAY, embedGateway);
  }
}

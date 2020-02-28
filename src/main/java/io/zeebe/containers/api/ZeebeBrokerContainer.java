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
package io.zeebe.containers.api;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.Network;

public interface ZeebeBrokerContainer<SELF extends ZeebeBrokerContainer<SELF>>
    extends ZeebeContainer<SELF>, Container<SELF> {

  SELF withBrokerNodeId(int nodeId);

  SELF withBrokerHost(String host);

  SELF withPortOffset(int portOffset);

  SELF withContactPoints(List<String> contactPoints);

  SELF withReplicationFactor(int replicationFactor);

  SELF withPartitionCount(int partitionCount);

  SELF withClusterSize(int clusterSize);

  SELF withEmbeddedGateway(boolean startEmbeddedGateway);

  SELF withDebugFlag(boolean debugFlag);

  SELF withGatewayHost(final String host);

  SELF withGatewayPort(final int port);

  SELF withGatewayKeepAliveInterval(final Duration keepAliveInterval);

  SELF withGatewayRequestTimeout(final Duration requestTimeout);

  SELF withGatewayManagementThreadCount(final int managementThreadCount);

  SELF withNetwork(Network network);

  SELF withConfiguration(final InputStream configuration);

  SELF withClusterName(final String clusterName);

  SELF withExposedPorts(Integer... ports);
}

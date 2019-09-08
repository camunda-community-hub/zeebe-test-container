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

import io.zeebe.containers.ZeebeEnvironment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ZeebeBrokerEnvironment extends ZeebeEnvironment<ZeebeBrokerEnvironment> {
  private String host;
  private int portOffset;

  private int nodeId;
  private int clusterSize;
  private String clusterName;
  private List<String> contactPoints;

  private int partitionCount;
  private int replicationFactor;

  private boolean debug;
  private boolean embedGateway;

  public String getHost() {
    return host;
  }

  public ZeebeBrokerEnvironment withHost(String host) {
    envMap.put(ZeebeBrokerEnvVar.HOST, host);
    this.host = host;
    return this;
  }

  public int getNodeId() {
    return nodeId;
  }

  public ZeebeBrokerEnvironment withNodeId(int nodeId) {
    envMap.put(ZeebeBrokerEnvVar.NODE_ID, nodeId);
    this.nodeId = nodeId;
    return this;
  }

  public int getPortOffset() {
    return portOffset;
  }

  public ZeebeBrokerEnvironment withPortOffset(int portOffset) {
    envMap.put(ZeebeBrokerEnvVar.PORT_OFFSET, portOffset);
    this.portOffset = portOffset;
    return this;
  }

  public int getReplicationFactor() {
    return replicationFactor;
  }

  public ZeebeBrokerEnvironment withReplicationFactor(int replicationFactor) {
    envMap.put(ZeebeBrokerEnvVar.REPLICATION_FACTOR, replicationFactor);
    this.replicationFactor = replicationFactor;
    return this;
  }

  public int getPartitionCount() {
    return partitionCount;
  }

  public ZeebeBrokerEnvironment withPartitionCount(int partitionCount) {
    envMap.put(ZeebeBrokerEnvVar.PARTITION_COUNT, partitionCount);
    this.partitionCount = partitionCount;
    return this;
  }

  public int getClusterSize() {
    return clusterSize;
  }

  public ZeebeBrokerEnvironment withClusterSize(int clusterSize) {
    envMap.put(ZeebeBrokerEnvVar.CLUSTER_SIZE, clusterSize);
    this.clusterSize = clusterSize;
    return this;
  }

  public String getClusterName() {
    return clusterName;
  }

  public ZeebeBrokerEnvironment withClusterName(String clusterName) {
    envMap.put(ZeebeBrokerEnvVar.CLUSTER_NAME, clusterName);
    this.clusterName = clusterName;
    return this;
  }

  public List<String> getContactPoints() {
    return contactPoints;
  }

  public ZeebeBrokerEnvironment withContactPoints(Collection<String> contactPoints) {
    envMap.put(ZeebeBrokerEnvVar.CONTACT_POINTS, contactPoints);
    this.contactPoints = new ArrayList<>(contactPoints);
    return this;
  }

  public boolean isDebug() {
    return debug;
  }

  public ZeebeBrokerEnvironment withDebug(boolean debug) {
    envMap.put(ZeebeBrokerEnvVar.DEBUG, debug);
    this.debug = debug;
    return this;
  }

  public boolean shouldEmbedGateway() {
    return embedGateway;
  }

  public ZeebeBrokerEnvironment withEmbeddedGateway(boolean embedGateway) {
    envMap.put(ZeebeBrokerEnvVar.EMBED_GATEWAY, embedGateway);
    this.embedGateway = embedGateway;
    return this;
  }
}

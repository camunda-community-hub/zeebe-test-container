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
package io.zeebe.containers.generation2;

import io.zeebe.containers.api.ZeebeBrokerEnvironment;
import io.zeebe.containers.impl.EnvVar;

public class Gen2ZeebeBrokerEnvironment extends Gen2ZeebeEnvironment
    implements ZeebeBrokerEnvironment {

  private static final EnvVar DEBUG_FLAG = new EnvVar("ZEEBE_DEBUG");
  private static final EnvVar EMBED_GATEWAY_FLAG = new EnvVar("ZEEBEBROKER_GATEWAY_ENABLE");

  private static final EnvVar BROKER_NODE_ID = new EnvVar("ZEEBEBROKER_CLUSTER_NODEID");
  private static final EnvVar CONTACT_POINTS =
      new EnvVar("ZEEBEBROKER_CLUSTER_INITIALCONTACTPOINTS");
  private static final EnvVar PARTITION_COUNT = new EnvVar("ZEEBEBROKER_CLUSTER_PARTITIONSCOUNT");
  private static final EnvVar REPLICATION_FACTOR =
      new EnvVar("ZEEBEBROKER_CLUSTER_REPLICATIONFACTOR");
  private static final EnvVar CLUSTER_SIZE = new EnvVar("ZEEBEBROKER_CLUSTER_SIZE");
  private static final EnvVar CLUSTER_NAME = new EnvVar("ZEEBEBROKER_CLUSTER_NAME");

  private static final EnvVar BROKER_HOST = new EnvVar("ZEEBEBROKER_NETWORK_HOST");
  private static final EnvVar PORT_OFFSET = new EnvVar("ZEEBEBROKER_NETWORK_PORTOFFSET");

  private static final EnvVar GATEWAY_HOST = new EnvVar("ZEEBEBROKER_GATEWAY_NETWORK_HOST");
  private static final EnvVar GATEWAY_PORT = new EnvVar("ZEEBEBROKER_GATEWAY_NETWORK_PORT");
  private static final EnvVar GATEWAY_KEEP_ALIVE_INTERVAL =
      new EnvVar("ZEEBEBROKER_GATEWAY_NETWORK_MINKEEPALIVEINTERVAL");
  private static final EnvVar GATEWAY_CLUSTER_NAME =
      new EnvVar("ZEEBEBROKER_GATEWAY_CLUSTER_CLUSTERNAME");
  private static final EnvVar GATEWAY_CLUSTER_MEMBER_ID =
      new EnvVar("ZEEBEBROKER_GATEWAY_CLUSTER_MEMBERID");
  private static final EnvVar GATEWAY_CLUSTER_HOST = new EnvVar("ZEEBEBROKER_GATEWAY_CLUSTER_HOST");
  private static final EnvVar GATEWAY_CLUSTER_PORT = new EnvVar("ZEEBEBROKER_GATEWAY_CLUSTER_PORT");
  private static final EnvVar GATEWAY_CONTACT_POINT =
      new EnvVar("ZEEBEBROKER_GATEWAY_CLUSTER_CONTACTPOINT");
  private static final EnvVar GATEWAY_REQUEST_TIMEOUT =
      new EnvVar("ZEEBEBROKER_GATEWAY_CLUSTER_REQUESTTIMEOUT");
  private static final EnvVar GATEWAY_MANAGEMENT_THREADS =
      new EnvVar("ZEEBEBROKER_GATEWAY_THREADS_MANAGEMENTTHREADS");

  @Override
  public EnvVar getDebugFlag() {
    return DEBUG_FLAG;
  }

  @Override
  public EnvVar getEmbedGatewayFlag() {
    return EMBED_GATEWAY_FLAG;
  }

  @Override
  public EnvVar getBrokerNodeId() {
    return BROKER_NODE_ID;
  }

  @Override
  public EnvVar getBrokerHost() {
    return BROKER_HOST;
  }

  @Override
  public EnvVar getPortOffset() {
    return PORT_OFFSET;
  }

  @Override
  public EnvVar getContactPoints() {
    return CONTACT_POINTS;
  }

  @Override
  public EnvVar getPartitionsCount() {
    return PARTITION_COUNT;
  }

  @Override
  public EnvVar getReplicationFactor() {
    return REPLICATION_FACTOR;
  }

  @Override
  public EnvVar getClusterSize() {
    return CLUSTER_SIZE;
  }

  @Override
  public EnvVar getClusterName() {
    return CLUSTER_NAME;
  }

  @Override
  public EnvVar getGatewayHost() {
    return GATEWAY_HOST;
  }

  @Override
  public EnvVar getGatewayPort() {
    return GATEWAY_PORT;
  }

  @Override
  public EnvVar getGatewayKeepAliveInterval() {
    return GATEWAY_KEEP_ALIVE_INTERVAL;
  }

  @Override
  public EnvVar getGatewayRequestTimeout() {
    return GATEWAY_REQUEST_TIMEOUT;
  }

  @Override
  public EnvVar getGatewayManagementThreadCount() {
    return GATEWAY_MANAGEMENT_THREADS;
  }
}

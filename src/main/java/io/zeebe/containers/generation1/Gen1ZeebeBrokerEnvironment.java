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
package io.zeebe.containers.generation1;

import io.zeebe.containers.api.EnvVar;
import io.zeebe.containers.api.ZeebeBrokerEnvironment;

public class Gen1ZeebeBrokerEnvironment extends Gen1ZeebeGatewayEnvironment
    implements ZeebeBrokerEnvironment {

  private static final EnvVar DEBUG_FLAG = new EnvVar("ZEEBE_DEBUG");
  private static final EnvVar EMBED_GATEWAY_FLAG = new EnvVar("ZEEBE_EMBED_GATEWAY");

  private static final EnvVar BROKER_NODE_ID = new EnvVar("ZEEBE_NODE_ID");
  private static final EnvVar BROKER_HOST = new EnvVar("ZEEBE_HOST");
  private static final EnvVar PORT_OFFSET = new EnvVar("ZEEBE_PORT_OFFSET");
  private static final EnvVar CONTACT_POINTS = new EnvVar("ZEEBE_CONTACT_POINTS");
  private static final EnvVar PARTITION_COUNT = new EnvVar("ZEEBE_PARTITIONS_COUNT");
  private static final EnvVar REPLICATION_FACTOR = new EnvVar("ZEEBE_REPLICATION_FACTOR");
  private static final EnvVar CLUSTER_SIZE = new EnvVar("ZEEBE_CLUSTER_SIZE");
  private static final EnvVar CLUSTER_NAME = new EnvVar("ZEEBE_CLUSTER_NAME");

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
}

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

public enum ZeebeBrokerEnvironment implements Environment {
  NODE_ID("ZEEBE_NODE_ID"),
  HOST("ZEEBE_HOST"),
  PORT_OFFSET("ZEEBE_PORT_OFFSET"),
  CONTACT_POINTS("ZEEBE_CONTACT_POINTS"),
  PARTITION_COUNT("ZEEBE_PARTITIONS_COUNT"),
  REPLICATION_FACTOR("ZEEBE_REPLICATION_FACTOR"),
  CLUSTER_SIZE("ZEEBE_CLUSTER_SIZE"),
  CLUSTER_NAME("ZEEBE_CLUSTER_NAME"),
  EMBED_GATEWAY("ZEEBE_EMBED_GATEWAY"),
  DEBUG("ZEEBE_DEBUG");

  private final String variable;

  ZeebeBrokerEnvironment(final String variable) {
    this.variable = variable;
  }

  @Override
  public String variable() {
    return variable;
  }
}

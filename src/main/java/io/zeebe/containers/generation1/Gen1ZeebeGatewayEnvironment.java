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
import io.zeebe.containers.api.ZeebeGatewayEnvironment;

public class Gen1ZeebeGatewayEnvironment extends Gen1ZeebeEnvironment
    implements ZeebeGatewayEnvironment {

  private static final EnvVar STANDALONE_GATEWAY = new EnvVar("ZEEBE_STANDALONE_GATEWAY");

  private static final EnvVar GATEWAY_HOST = new EnvVar("ZEEBE_GATEWAY_HOST");
  private static final EnvVar GATEWAY_PORT = new EnvVar("ZEEBE_GATEWAY_PORT");
  private static final EnvVar GATEWAY_CLUSTER_NAME = new EnvVar("ZEEBE_GATEWAY_CLUSTER_NAME");
  private static final EnvVar GATEWAY_CLUSTER_MEMBER_ID =
      new EnvVar("ZEEBE_GATEWAY_CLUSTER_MEMBER_ID");
  private static final EnvVar GATEWAY_CLUSTER_HOST = new EnvVar("ZEEBE_GATEWAY_CLUSTER_HOST");
  private static final EnvVar GATEWAY_CLUSTER_PORT = new EnvVar("ZEEBE_GATEWAY_CLUSTER_PORT");
  private static final EnvVar GATEWAY_KEEP_ALIVE_INTERVAL =
      new EnvVar("ZEEBE_GATEWAY_KEEP_ALIVE_INTERVAL");
  private static final EnvVar GATEWAY_CONTACT_POINT = new EnvVar("ZEEBE_GATEWAY_CONTACT_POINT");
  private static final EnvVar GATEWAY_REQUEST_TIMEOUT = new EnvVar("ZEEBE_GATEWAY_REQUEST_TIMEOUT");
  private static final EnvVar GATEWAY_MANAGEMENT_THREADS =
      new EnvVar("ZEEBE_GATEWAY_MANAGEMENT_THREADS");

  @Override
  public EnvVar getStandaloneGateway() {
    return STANDALONE_GATEWAY;
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
  public EnvVar getGatewayClusterName() {
    return GATEWAY_CLUSTER_NAME;
  }

  @Override
  public EnvVar getGatewayClusterMemberId() {
    return GATEWAY_CLUSTER_MEMBER_ID;
  }

  @Override
  public EnvVar getGatewayClusterHost() {
    return GATEWAY_CLUSTER_HOST;
  }

  @Override
  public EnvVar getGatewayClusterPort() {
    return GATEWAY_CLUSTER_PORT;
  }

  @Override
  public EnvVar getGatewayKeepAliveInterval() {
    return GATEWAY_KEEP_ALIVE_INTERVAL;
  }

  @Override
  public EnvVar getGatewayContactPoint() {
    return GATEWAY_CONTACT_POINT;
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

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

import io.zeebe.containers.api.ZeebeGatewayEnvironment;
import io.zeebe.containers.impl.EnvVar;

public class Gen2ZeebeGatewayEnvironment extends Gen2ZeebeEnvironment
    implements ZeebeGatewayEnvironment {

  private static final EnvVar STANDALONE_GATEWAY = new EnvVar("ZEEBE_STANDALONE_GATEWAY");

  private static final EnvVar GATEWAY_HOST = new EnvVar("ZEEBEGATEWAY_NETWORK_HOST");
  private static final EnvVar GATEWAY_PORT = new EnvVar("ZEEBEGATEWAY_NETWORK_PORT");
  private static final EnvVar GATEWAY_KEEP_ALIVE_INTERVAL =
      new EnvVar("ZEEBEGATEWAY_NETWORK_MINKEEPALIVEINTERVAL");
  private static final EnvVar GATEWAY_CLUSTER_NAME = new EnvVar("ZEEBEGATEWAY_CLUSTER_CLUSTERNAME");
  private static final EnvVar GATEWAY_CLUSTER_MEMBER_ID =
      new EnvVar("ZEEBEGATEWAY_CLUSTER_MEMBERID");
  private static final EnvVar GATEWAY_CLUSTER_HOST = new EnvVar("ZEEBEGATEWAY_CLUSTER_HOST");
  private static final EnvVar GATEWAY_CLUSTER_PORT = new EnvVar("ZEEBEGATEWAY_CLUSTER_PORT");
  private static final EnvVar GATEWAY_CONTACT_POINT =
      new EnvVar("ZEEBEGATEWAY_CLUSTER_CONTACTPOINT");
  private static final EnvVar GATEWAY_REQUEST_TIMEOUT =
      new EnvVar("ZEEBEGATEWAY_CLUSTER_REQUESTTIMEOUT");
  private static final EnvVar GATEWAY_MANAGEMENT_THREADS =
      new EnvVar("ZEEBEGATEWAY_THREADS_MANAGEMENTTHREADS");

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
    return GATEWAY_CLUSTER_HOST;
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

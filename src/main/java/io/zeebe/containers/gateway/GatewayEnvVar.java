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
package io.zeebe.containers.gateway;

import io.zeebe.containers.EnvVar;

public enum GatewayEnvVar implements EnvVar {
  STANDALONE("ZEEBE_STANDALONE_GATEWAY"),
  HOST("ZEEBE_GATEWAY_HOST"),
  PORT("ZEEBE_GATEWAY_PORT"),
  CONTACT_POINT("ZEEBE_GATEWAY_CONTACT_POINT"),
  TRANSPORT_BUFFER("ZEEBE_GATEWAY_TRANSPORT_BUFFER"),
  REQUEST_TIMEOUT("ZEEBE_GATEWAY_REQUEST_TIMEOUT"),
  CLUSTER_NAME("ZEEBE_GATEWAY_CLUSTER_NAME"),
  CLUSTER_MEMBER_ID("ZEEBE_GATEWAY_CLUSTER_MEMBER_ID"),
  CLUSTER_HOST("ZEEBE_GATEWAY_CLUSTER_HOST"),
  CLUSTER_PORT("ZEEBE_GATEWAY_CLUSTER_PORT"),
  MANAGEMENT_THREAD_COUNT("ZEEBE_GATEWAY_MANAGEMENT_THREADS"),
  SECURITY_ENABLED("ZEEBE_GATEWAY_SECURITY_ENABLED"),
  CERTIFICATE_PATH("ZEEBE_GATEWAY_CERTIFICATE_PATH"),
  PRIVATE_KEY_PATH("ZEEBE_GATEWAY_PRIVATE_KEY_PATH"),
  MONITORING_ENABLED("ZEEBE_GATEWAY_MONITORING_ENABLED"),
  MONITORING_HOST("ZEEBE_GATEWAY_MONITORING_HOST"),
  MONITORING_PORT("ZEEBE_GATEWAY_MONITORING_PORT");

  private final String variableName;

  GatewayEnvVar(String variableName) {
    this.variableName = variableName;
  }

  @Override
  public String getVariableName() {
    return variableName;
  }
}

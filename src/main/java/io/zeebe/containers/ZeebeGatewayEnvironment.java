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

public enum ZeebeGatewayEnvironment implements Environment {
  HOST("ZEEBE_GATEWAY_HOST"),
  PORT("ZEEBE_GATEWAY_PORT"),
  CONTACT_POINT("ZEEBE_GATEWAY_CONTACT_POINT"),
  TRANSPORT_BUFFER("ZEEBE_GATEWAY_TRANSPORT_BUFFER"),
  REQUEST_TIMEOUT("ZEEBE_GATEWAY_REQUEST_TIMEOUT"),
  MANAGEMENT_THREAD_COUNT("ZEEBE_GATEWAY_MANAGEMENT_THREADS"),
  SECURITY_ENABLED("ZEEBE_GATEWAY_SECURITY_ENABLED"),
  CERTIFICATE_PATH("ZEEBE_GATEWAY_CERTIFICATE_PATH"),
  PRIVATE_KEY_PATH("ZEEBE_GATEWAY_PRIVATE_KEY_PATH"),
  MONITORING_ENABLED("ZEEBE_GATEWAY_MONITORING_ENABLED"),
  MONITORING_HOST("ZEEBE_GATEWAY_MONITORING_HOST"),
  MONITORING_PORT("ZEEBE_GATEWAY_MONITORING_PORT"),
  KEEP_ALIVE_INTERVAL("ZEEBE_GATEWAY_KEEP_ALIVE_INTERVAL"),
  MAX_MESSAGE_SIZE("ZEEBE_GATEWAY_MAX_MESSAGE_SIZE"),
  MAX_MESSAGE_COUNT("ZEEBE_GATEWAY_MAX_MESSAGE_COUNT");

  private final String variable;

  ZeebeGatewayEnvironment(final String variable) {
    this.variable = variable;
  }

  @Override
  public String variable() {
    return variable;
  }
}

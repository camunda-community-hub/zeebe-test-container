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

public enum ZeebeStandaloneGatewayEnvironment implements Environment {
  STANDALONE("ZEEBE_STANDALONE_GATEWAY"),
  CLUSTER_NAME("ZEEBE_GATEWAY_CLUSTER_NAME"),
  CLUSTER_MEMBER_ID("ZEEBE_GATEWAY_CLUSTER_MEMBER_ID"),
  CLUSTER_HOST("ZEEBE_GATEWAY_CLUSTER_HOST"),
  CLUSTER_PORT("ZEEBE_GATEWAY_CLUSTER_PORT");

  private final String variable;

  ZeebeStandaloneGatewayEnvironment(String variable) {
    this.variable = variable;
  }

  @Override
  public String variable() {
    return variable;
  }
}

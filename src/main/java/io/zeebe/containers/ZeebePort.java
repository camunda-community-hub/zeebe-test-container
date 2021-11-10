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

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/** Represents the list of known port in a Zeebe cluster, all node types included. */
@API(status = Status.STABLE)
public enum ZeebePort {
  /** Port of the command API, i.e. the port used by the gateway to communicate with the broker */
  COMMAND(26501),
  /** Port of the gateway API, i.e. the port used by the client to communicate with any gateway */
  GATEWAY(26500),
  /** Port for internal communication, i.e. what all nodes use to communicate for clustering */
  INTERNAL(26502),
  /** Port for the management server, i.e. actuators, metrics, etc. */
  MONITORING(9600);

  private final int port;

  ZeebePort(final int port) {
    this.port = port;
  }

  /** @return returns the default port number for this port */
  public int getPort() {
    return port;
  }
}

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

import org.testcontainers.containers.GenericContainer;

/**
 * Represents common properties of all Zeebe brokers in a cluster, e.g. the command API address,
 * etc.
 *
 * @param <T> the concrete type of the underlying container
 */
public interface ZeebeBrokerNode<T extends GenericContainer<T>> extends ZeebeNode<T> {
  /**
   * Returns the address to access the command API of this node from within the same container
   * network as this node's.
   *
   * @return the internal command address
   */
  default String getInternalCommandAddress() {
    return getInternalAddress(ZeebePort.COMMAND.getPort());
  }

  /**
   * Returns the address the command API of this broker from outside of its container network.
   *
   * @return the external command API address
   */
  default String getExternalCommandAddress() {
    return getExternalAddress(ZeebePort.COMMAND.getPort());
  }
}

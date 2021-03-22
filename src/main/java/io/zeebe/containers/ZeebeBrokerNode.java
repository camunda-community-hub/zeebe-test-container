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
import org.testcontainers.containers.GenericContainer;

/**
 * Represents common properties of all Zeebe brokers in a cluster, e.g. the command API address,
 * etc.
 *
 * @param <T> the concrete type of the underlying container
 */
@API(status = Status.STABLE)
public interface ZeebeBrokerNode<T extends GenericContainer<T> & ZeebeBrokerNode<T>>
    extends ZeebeNode<T> {

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

  /**
   * Allows reuse of the broker data across restarts by attaching the data folder to any valid
   * implementation of {@link ZeebeData}, e.g. a Docker volume (see {@link ZeebeVolume} or a path on
   * the host node (see {@link ZeebeHostData}).
   *
   * NOTE: the container itself does not manage the given resource, so you should keep track of it
   * and close it if need be. In the case of {@link ZeebeVolume}, the implementation is aware of the
   * Testcontainers resource reaper, such that if your JVM crashes, the volume will eventually be
   * reaped anyway.
   *
   * For example, if you want to test updating a broker, you could do the following: <pre>{@code
   *     final DockerImageName oldImage = DockerImageName.parse("camunda/zeebe:1.0.0");
   *     final DockerImageName newImage = DockerImageName.parse("camunda/zeebe:1.1.0");
   *     final ZeebeVolume volume = new ZeebeVolume();
   *     final ZeebeBrokerContainer broker = new ZeebeBrokerContainer(oldImage)
   *        .withZeebeData(volume);
   *
   *     // do stuff on the broker, then stop it
   *     broker.stop();
   *     broker.setDockerImage(newImage);
   *     broker.start();
   *
   *     // verify state is correct after update
   *  }
   *
   * @param data the data implementation to use
   * @return this container for chaining
   */
  @API(status = Status.EXPERIMENTAL)
  default T withZeebeData(final ZeebeData data) {
    data.attach(self());
    return self();
  }
}

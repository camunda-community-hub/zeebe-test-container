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
package io.zeebe.containers.api;

import java.io.File;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collection;
import org.slf4j.event.Level;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.Network;

public interface ZeebeContainer<SELF extends ZeebeContainer<SELF>>
    extends Container<SELF>, ZeebeNetworkable {

  SELF withLogLevel(final Level logLevel);

  SELF withAtomixLogLevel(final Level logLevel);

  SELF withClusterName(final String clusterName);

  SELF withConfigurationResource(final String configurationResource);

  SELF withConfigurationFile(final File configurationFile);

  SELF withConfiguration(final InputStream configuration);

  SELF withAdvertisedHost(final String advertisedHost);

  default SELF withEnv(final EnvVar envVar, final String value) {
    return withEnv(envVar.variable(), value);
  }

  default SELF withEnv(final EnvVar envVar, final boolean value) {
    return withEnv(envVar, String.valueOf(value));
  }

  default SELF withEnv(final EnvVar envVar, final int value) {
    return withEnv(envVar, String.valueOf(value));
  }

  default SELF withEnv(final EnvVar envVar, final Duration value) {
    return withEnv(envVar, value.toString());
  }

  default SELF withEnv(final EnvVar envVar, final Collection<String> value) {
    return withEnv(envVar, String.join(",", value));
  }

  /**
   * Attempts to stop the container gracefully. If it times out, the container is abruptly killed.
   *
   * @param timeout must be greater than 1 second
   */
  default void shutdownGracefully(Duration timeout) {
    getDockerClient()
        .stopContainerCmd(getContainerId())
        .withTimeout((int) timeout.getSeconds())
        .exec();
  }

  void start();

  void stop();

  Network getNetwork();
}

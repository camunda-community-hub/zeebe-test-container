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

import io.zeebe.containers.impl.EnvVar;
import java.io.File;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.event.Level;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.SelinuxContext;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.images.ImagePullPolicy;
import org.testcontainers.utility.MountableFile;

public interface ZeebeBrokerContainer<SELF extends ZeebeBrokerContainer<SELF>>
    extends Container<SELF>, ZeebeContainer<SELF> {

  SELF withBrokerNodeId(int nodeId);

  SELF withBrokerHost(String host);

  SELF withPortOffset(int portOffset);

  SELF withContactPoints(List<String> contactPoints);

  SELF withReplicationFactor(int replicationFactor);

  SELF withPartitionCount(int partitionCount);

  SELF withClusterSize(int clusterSize);

  SELF withEmbeddedGateway(boolean startEmbeddedGateway);

  SELF withDebugFlag(boolean debugFlag);

  SELF withGatewayHost(final String host);

  SELF withGatewayPort(final int port);

  SELF withGatewayKeepAliveInterval(final Duration keepAliveInterval);

  SELF withGatewayRequestTimeout(final Duration requestTimeout);

  SELF withGatewayManagementThreadCount(final int managementThreadCount);

  /**
   * Below are copies of methods in superinterfaces. These copies are necessary, because otherwise
   * the type system will complain when this interface is used in a fluent way. Basically, the type
   * system recognizes only the SELF type of the interface where the method was defined.
   *
   * <p>Maybe there is a cleverer way to do this, but for now this is the only workaround we were
   * able to find.
   */

  // copied from ZeebeContainer
  SELF withLogLevel(final Level logLevel);

  SELF withAtomixLogLevel(final Level logLevel);

  SELF withClusterName(final String clusterName);

  SELF withConfigurationResource(final String configurationResource);

  SELF withConfigurationFile(final File configurationFile);

  SELF withConfiguration(final InputStream configuration);

  SELF withAdvertisedHost(final String advertisedHost);

  SELF withEnv(final EnvVar envVar, final String value);

  SELF withEnv(final EnvVar envVar, final boolean value);

  SELF withEnv(final EnvVar envVar, final int value);

  SELF withEnv(final EnvVar envVar, final Duration value);

  SELF withEnv(final EnvVar envVar, final Collection<String> value);

  // copied from Container
  SELF waitingFor(WaitStrategy var1);

  default SELF withFileSystemBind(String hostPath, String containerPath) {
    return this.withFileSystemBind(hostPath, containerPath, BindMode.READ_WRITE);
  }

  SELF withFileSystemBind(String var1, String var2, BindMode var3);

  SELF withVolumesFrom(Container var1, BindMode var2);

  SELF withExposedPorts(Integer... var1);

  SELF withCopyFileToContainer(MountableFile var1, String var2);

  SELF withEnv(String var1, String var2);

  default SELF withEnv(String key, Function<Optional<String>, String> mapper) {
    final Optional<String> oldValue = Optional.ofNullable(this.getEnvMap().get(key));
    return this.withEnv(key, (String) mapper.apply(oldValue));
  }

  SELF withEnv(Map<String, String> var1);

  SELF withLabel(String var1, String var2);

  SELF withLabels(Map<String, String> var1);

  SELF withCommand(String var1);

  SELF withCommand(String... var1);

  SELF withExtraHost(String var1, String var2);

  SELF withNetworkMode(String var1);

  SELF withNetwork(Network var1);

  SELF withNetworkAliases(String... var1);

  SELF withImagePullPolicy(ImagePullPolicy var1);

  default SELF withClasspathResourceMapping(
      String resourcePath, String containerPath, BindMode mode) {
    this.withClasspathResourceMapping(resourcePath, containerPath, mode, SelinuxContext.NONE);
    return this.self();
  }

  SELF withClasspathResourceMapping(String var1, String var2, BindMode var3, SelinuxContext var4);

  SELF withStartupTimeout(Duration var1);

  SELF withPrivilegedMode(boolean var1);

  SELF withMinimumRunningDuration(Duration var1);

  SELF withStartupCheckStrategy(StartupCheckStrategy var1);

  SELF withWorkingDirectory(String var1);
}

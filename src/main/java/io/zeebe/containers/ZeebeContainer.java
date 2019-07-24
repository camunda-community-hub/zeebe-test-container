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

import java.util.Map;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.TestcontainersConfiguration;

public abstract class ZeebeContainer<
        E extends ContainerEnvironment, SELF extends ZeebeContainer<E, SELF>>
    extends GenericContainer<SELF> {

  protected static final String ZEEBE_CONTAINER_IMAGE_PROPERTY = "zeebe.container.image";
  protected static final String DEFAULT_ZEEBE_CONTAINER_IMAGE = "camunda/zeebe";
  protected static final String CONTAINER_CONFIGURATION_PATH =
      "/usr/local/zeebe/conf/zeebe.cfg.toml";
  protected static final String DEFAULT_ZEEBE_VERSION = "0.20.0";
  protected final E environment;

  public ZeebeContainer() {
    this(DEFAULT_ZEEBE_VERSION);
  }

  public ZeebeContainer(String version) {
    super(String.format("%s:%s", getZeebeContainerImage(), version));
    this.environment = newDefaultEnvironment();
    applyDefaultConfiguration();
  }

  public ZeebeContainer(String version, E environment) {
    super(String.format("%s:%s", getZeebeContainerImage(), version));
    this.environment = environment;
    applyDefaultConfiguration();
  }

  protected static String getZeebeContainerImage() {
    return TestcontainersConfiguration.getInstance()
        .getProperties()
        .getOrDefault(ZEEBE_CONTAINER_IMAGE_PROPERTY, DEFAULT_ZEEBE_CONTAINER_IMAGE)
        .toString();
  }

  public SELF withConfigurationResource(String resourceName) {
    return withClasspathResourceMapping(
        resourceName, CONTAINER_CONFIGURATION_PATH, BindMode.READ_ONLY);
  }

  public SELF withLogLevel(String logLevel) {
    return withEnv("ZEEBE_LOG_LEVEL", logLevel);
  }

  public String getInternalAddress(ZeebePort port) {
    return String.format("%s:%d", getInternalHost(), port.getPort());
  }

  public String getExternalAddress(ZeebePort port) {
    return String.format("%s:%d", getContainerIpAddress(), getMappedPort(port.getPort()));
  }

  public E getEnvironment() {
    return environment;
  }

  protected abstract int getPort(ZeebePort port);

  protected abstract E newDefaultEnvironment();

  protected abstract Map<ZeebePort, Integer> getPorts();

  protected abstract String getInternalHost();

  @Override
  protected void doStart() {
    withEnv(environment.getEnvMap());
    withExposedPorts(getPorts().values().toArray(new Integer[0]));
    withNetworkAliases(getInternalHost());

    super.doStart();
  }

  protected void applyDefaultConfiguration() {
    withNetwork(Network.newNetwork());
  }
}

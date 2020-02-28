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

import de.skuzzle.semantic.Version;
import io.zeebe.containers.api.ZeebeBrokerEnvironment;
import io.zeebe.containers.impl.AbstractZeebeBrokerContainer;
import java.io.File;
import java.io.InputStream;

public class Gen2ZeebeBrokerContainer
    extends AbstractZeebeBrokerContainer<Gen2ZeebeBrokerContainer> {

  private static final ZeebeBrokerEnvironment BROKER_ENVIRONMENT = new Gen2ZeebeBrokerEnvironment();

  public Gen2ZeebeBrokerContainer() {
    this(Gen2ZeebeDefaults.DEFAULT_ZEEBE_VERSION);
  }

  public Gen2ZeebeBrokerContainer(final Version version) {
    super(version);
  }

  public Gen2ZeebeBrokerContainer(final String dockerImageNameWithTag) {
    super(dockerImageNameWithTag);
  }

  @Override
  protected ZeebeBrokerEnvironment getZeebeBrokerEnvironment() {
    return BROKER_ENVIRONMENT;
  }

  @Override
  protected String getConfigFilePath() {
    return Gen2ZeebeDefaults.DEFAULT_CONFIGURATION_PATH_GATEWAY;
  }

  @Override
  public Gen2ZeebeBrokerContainer withConfigurationFile(final File configurationFile) {
    withConfigurationFileLocation(getConfigFilePath());
    return super.withConfigurationFile(configurationFile);
  }

  @Override
  public Gen2ZeebeBrokerContainer withConfigurationResource(final String configurationResource) {
    withConfigurationFileLocation(getConfigFilePath());
    return super.withConfigurationResource(configurationResource);
  }

  /**
   * Sets the location of the configuration file. This happens automatically when {@link
   * Gen2ZeebeBrokerContainer#withConfigurationResource(String)} }, {@link
   * Gen2ZeebeBrokerContainer#withConfigurationFile(File)} or {@link
   * Gen2ZeebeBrokerContainer#withConfiguration(InputStream)} is used. However, when a config file
   * is uploaded via {@Link
   * org.testcontainers.containers.GenericContainer#withCopyFileToContainer(org.testcontainers.utility.MountableFile,
   * java.lang.String)} then this config file must be registered with this method
   *
   * @param configFileLocation location of the config file within the container
   */
  public Gen2ZeebeBrokerContainer withConfigurationFileLocation(String configFileLocation) {
    withEnv(Gen2ZeebeDefaults.CONFIG_FILE_LOCATION, "file:" + configFileLocation);
    return this;
  }
}

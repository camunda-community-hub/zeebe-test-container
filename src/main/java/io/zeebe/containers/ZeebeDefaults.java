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
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * Represents a set of static defaults regarding the Zeebe docker image, accessible via a singleton
 * instance, {@link #getInstance()}.
 */
@API(status = Status.STABLE)
@SuppressWarnings({"squid:S1075", "WeakerAccess"})
public final class ZeebeDefaults {
  private static final String ZEEBE_CONTAINER_IMAGE_PROPERTY = "zeebe.container.image";
  private static final String DEFAULT_ZEEBE_CONTAINER_IMAGE = "camunda/zeebe";
  private static final String ZEEBE_CONTAINER_VERSION_PROPERTY = "zeebe.container.version";
  private static final String DEFAULT_ZEEBE_VERSION = "0.26.1";
  private static final String DEFAULT_ZEEBE_DATA_PATH = "/usr/local/zeebe/data";

  private ZeebeDefaults() {}

  /** @return the singleton instance */
  public static ZeebeDefaults getInstance() {
    return Singleton.INSTANCE;
  }

  /** @return the default Zeebe docker image, without a tag */
  public String getDefaultImage() {
    return TestcontainersConfiguration.getInstance()
        .getEnvVarOrProperty(ZEEBE_CONTAINER_IMAGE_PROPERTY, DEFAULT_ZEEBE_CONTAINER_IMAGE);
  }

  /** @return the default Zeebe docker image tag/version */
  public String getDefaultVersion() {
    return TestcontainersConfiguration.getInstance()
        .getEnvVarOrProperty(ZEEBE_CONTAINER_VERSION_PROPERTY, DEFAULT_ZEEBE_VERSION);
  }

  /** @return the default Zeebe docker image */
  public DockerImageName getDefaultDockerImage() {
    return DockerImageName.parse(getDefaultImage()).withTag(getDefaultVersion());
  }

  public String getDefaultDataPath() {
    return DEFAULT_ZEEBE_DATA_PATH;
  }

  private static final class Singleton {

    private static final ZeebeDefaults INSTANCE = new ZeebeDefaults();
  }
}

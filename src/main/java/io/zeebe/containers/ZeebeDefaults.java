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

import org.testcontainers.utility.TestcontainersConfiguration;

@SuppressWarnings("squid:S1075")
public final class ZeebeDefaults {
  private static final String DEFAULT_CLUSTER_NAME = "zeebe";
  private static final String ZEEBE_CONTAINER_IMAGE_PROPERTY = "zeebe.container.image";
  private static final String DEFAULT_ZEEBE_CONTAINER_IMAGE = "camunda/zeebe";
  private static final String DEFAULT_ZEEBE_VERSION = "0.21.0-alpha2";
  private static final String DEFAULT_CONFIGURATION_PATH = "/usr/local/zeebe/conf/zeebe.cfg.toml";

  private static final ZeebeDefaults INSTANCE = new ZeebeDefaults();

  private ZeebeDefaults() {}

  public static ZeebeDefaults getInstance() {
    return INSTANCE;
  }

  public String getDefaultImage() {
    return TestcontainersConfiguration.getInstance()
        .getProperties()
        .getOrDefault(ZEEBE_CONTAINER_IMAGE_PROPERTY, DEFAULT_ZEEBE_CONTAINER_IMAGE)
        .toString();
  }

  public String getDefaultVersion() {
    return DEFAULT_ZEEBE_VERSION;
  }

  public String getDefaultClusterName() {
    return DEFAULT_CLUSTER_NAME;
  }

  public String getDefaultConfigurationPath() {
    return DEFAULT_CONFIGURATION_PATH;
  }
}

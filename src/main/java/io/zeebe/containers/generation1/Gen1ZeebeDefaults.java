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
package io.zeebe.containers.generation1;

import de.skuzzle.semantic.Version;

@SuppressWarnings({"squid:S1075", "WeakerAccess"})
public final class Gen1ZeebeDefaults {
  static final Version DEFAULT_ZEEBE_VERSION = Version.parseVersion("0.21.0-alpha2");
  static final String DEFAULT_CONFIGURATION_PATH_BROKER = "/usr/local/zeebe/conf/zeebe.cfg.toml";
  static final String DEFAULT_CONFIGURATION_PATH_GATEWAY = "/usr/local/zeebe/conf/gateway.cfg.toml";
}

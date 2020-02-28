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

import io.zeebe.containers.api.EnvVar;
import io.zeebe.containers.api.ZeebeEnvironment;

public class Gen1ZeebeEnvironment implements ZeebeEnvironment {

  private static final EnvVar ZEEBE_LOG_LEVEL = new EnvVar("ZEEBE_LOG_LEVEL");
  private static final EnvVar ATOMIX_LOG_LEVEL = new EnvVar("ATOMIX_LOG_LEVEL");
  private static final EnvVar ADVERTISED_HOST = new EnvVar("ZEEBE_ADVERTISED_HOST");

  @Override
  public EnvVar getZeebeLogLevel() {
    return ZEEBE_LOG_LEVEL;
  }

  @Override
  public EnvVar getAtomixLogLevel() {
    return ATOMIX_LOG_LEVEL;
  }

  @Override
  public EnvVar getAdvertisedHost() {
    return ADVERTISED_HOST;
  }
}

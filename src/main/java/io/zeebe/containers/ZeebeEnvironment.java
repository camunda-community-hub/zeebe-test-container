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

import io.zeebe.containers.util.EnvMap;
import java.util.Map;
import org.slf4j.event.Level;

public abstract class ZeebeEnvironment<SELF extends ZeebeEnvironment<SELF>>
    implements ContainerEnvironment {
  protected final EnvMap envMap;

  private Level logLevel;
  private Level atomixLogLevel;

  public ZeebeEnvironment() {
    this.envMap = new EnvMap();
  }

  public Map<String, String> getEnvMap() {
    return envMap.getMap();
  }

  public Level getLogLevel() {
    return logLevel;
  }

  public SELF withLogLevel(Level logLevel) {
    this.logLevel = logLevel;
    return self();
  }

  public Level getAtomixLogLevel() {
    return atomixLogLevel;
  }

  public SELF withAtomixLogLevel(Level atomixLogLevel) {
    this.atomixLogLevel = atomixLogLevel;
    return self();
  }

  /** @return a reference to this container instance, cast to the expected generic type. */
  @SuppressWarnings("unchecked")
  private SELF self() {
    return (SELF) this;
  }
}

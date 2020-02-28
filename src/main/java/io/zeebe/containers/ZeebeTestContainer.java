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

import de.skuzzle.semantic.Version;
import io.zeebe.containers.api.ZeebeTestContainerFactory;
import io.zeebe.containers.generation1.Gen1ZeebeTestContainerFacory;
import io.zeebe.containers.generation2.Gen2ZeebeTestContainerFacory;

public class ZeebeTestContainer {

  private static final Version START_VERSION_GENERATION1 = Version.parseVersion("0.20.1");
  private static final Version START_VERSION_GENERATION2 = Version.parseVersion("0.23.0-alpha2");

  public static ZeebeTestContainerFactory getFactory(Version version) {
    if (version.isLowerThan(START_VERSION_GENERATION1)) {
      throw new IllegalArgumentException(
          "Version "
              + version
              + " currently not supported. Lowest supported version is "
              + START_VERSION_GENERATION1
              + ".");
    } else if (version.isLowerThan(START_VERSION_GENERATION2)) {
      return getGeneration1Factory(version);
    } else {
      return getGeneration2Factory(version);
    }
  }

  public static Gen1ZeebeTestContainerFacory getGeneration1Factory(Version version) {
    return new Gen1ZeebeTestContainerFacory(version);
  }

  public static Gen2ZeebeTestContainerFacory getGeneration2Factory(Version version) {
    return new Gen2ZeebeTestContainerFacory(version);
  }
}

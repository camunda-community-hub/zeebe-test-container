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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public enum SupportedVersion {
  ZEEBE_0_20_1("0.20.1", ".*Broker is ready.*"),
  ZEEBE_0_21_1("0.21.1", ".*Broker is ready.*"),
  ZEEBE_0_22_1("0.22.1", ".* succeeded. Started.*");

  private static final Map<String, SupportedVersion> LOOKUP_MAP = new HashMap<>();
  private final String version;
  private final String logStringRegex;

  static {
    Arrays.stream(SupportedVersion.values())
        .forEach(version -> LOOKUP_MAP.put(version.version(), version));
  }

  SupportedVersion(final String version, final String logStringRegex) {
    this.version = version;
    this.logStringRegex = logStringRegex;
  }

  public String version() {
    return version;
  }

  public String logStringRegex() {
    return logStringRegex;
  }

  public static SupportedVersion fromVersion(String version) {
    return LOOKUP_MAP.get(version);
  }

  @Override
  public String toString() {
    return version;
  }
}

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

import static de.skuzzle.semantic.Version.parseVersion;
import static org.testcontainers.shaded.org.apache.commons.lang.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import de.skuzzle.semantic.Version;
import org.testcontainers.shaded.org.apache.commons.lang.builder.ToStringBuilder;

public enum SupportedVersion {
  ZEEBE_0_20_1(parseVersion("0.20.1")),
  ZEEBE_0_21_1(parseVersion("0.21.1")),
  ZEEBE_0_22_1(parseVersion("0.22.1")),
  SNAPSHOT(parseVersion("0.22.1"), "SNAPSHOT");

  private final Version version;
  private final String tagName;

  SupportedVersion(final Version version) {
    this(version, version.toString());
  }

  SupportedVersion(final Version version, String tagName) {
    this.version = version;
    this.tagName = version.toString();
  }

  public Version version() {
    return version;
  }

  public String tagName() {
    return tagName;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
  }
}

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

public enum SupportedVersion {
  ZEEBE_0_20_1("0.20.1"),
  ZEEBE_0_21_1("0.21.1"),
  ZEEBE_0_22_0_alpha1("0.22.0-alpha1");

  private final String version;

  SupportedVersion(final String version) {
    this.version = version;
  }

  public String version() {
    return version;
  }

  @Override
  public String toString() {
    return version;
  }
}

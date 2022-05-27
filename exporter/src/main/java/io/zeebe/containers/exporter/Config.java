/*
 * Copyright © 2022 camunda services GmbH (info@camunda.com)
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
package io.zeebe.containers.exporter;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

/** The parsed configuration of the exporter. */
public record Config(URI endpointURI) {

  public Config(final URI endpointURI) {
    this.endpointURI = Objects.requireNonNull(endpointURI, "must specify URL");
  }

  static Config of(final Map<String, Object> args) {
    final String rawUrl = (String) args.getOrDefault("url", "");
    return new Config(URI.create(rawUrl));
  }
}

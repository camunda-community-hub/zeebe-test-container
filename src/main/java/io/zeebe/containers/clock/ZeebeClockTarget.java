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
package io.zeebe.containers.clock;

import feign.Request;
import feign.RequestTemplate;
import feign.Target;
import io.zeebe.containers.ZeebeNode;

final class ZeebeClockTarget implements Target<ZeebeClockClient> {
  private final ZeebeNode<?> zeebe;
  private final String scheme;

  ZeebeClockTarget(final ZeebeNode<?> zeebe) {
    this(zeebe, "http");
  }

  ZeebeClockTarget(final ZeebeNode<?> zeebe, final String scheme) {
    this.zeebe = zeebe;
    this.scheme = scheme;
  }

  @Override
  public Class<ZeebeClockClient> type() {
    return ZeebeClockClient.class;
  }

  @Override
  public String name() {
    return String.format("%s clock", zeebe.getInternalHost());
  }

  @Override
  public String url() {
    return String.format("%s://%s", scheme, zeebe.getExternalMonitoringAddress());
  }

  @Override
  public Request apply(final RequestTemplate input) {
    input.target(url());
    return input.request();
  }
}

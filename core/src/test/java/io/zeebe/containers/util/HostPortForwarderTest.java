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
package io.zeebe.containers.util;

import static org.assertj.core.api.Assertions.assertThat;

import io.zeebe.containers.util.HostPortForwarder.PortForwarder;
import java.util.HashSet;
import java.util.Set;
import org.agrona.collections.MutableInteger;
import org.junit.jupiter.api.Test;

final class HostPortForwarderTest {
  @Test
  void shouldRetryUpToRetryCount() {
    // given
    final int retryCount = 5;
    final MutableInteger attempts = new MutableInteger();
    final PortForwarder forwarder =
        mapping -> {
          if (attempts.incrementAndGet() >= retryCount) {
            return;
          }

          throw new RuntimeException("failure");
        };
    final HostPortForwarder portForwarder = new HostPortForwarder(forwarder);

    // when
    portForwarder.forward(1024, retryCount);

    // then
    assertThat(attempts.value).isEqualTo(retryCount);
  }

  @Test
  void shouldChangeContainerPortOnRetry() {
    // given
    final int retryCount = 5;
    final MutableInteger attempts = new MutableInteger();
    final Set<Integer> containerPorts = new HashSet<>();
    final PortForwarder forwarder =
        mapping -> {
          containerPorts.add(mapping.get(1024));

          if (attempts.incrementAndGet() >= retryCount) {
            return;
          }

          throw new RuntimeException("failure");
        };
    final HostPortForwarder portForwarder = new HostPortForwarder(forwarder);

    // when
    portForwarder.forward(1024, retryCount);

    // then
    assertThat(containerPorts).hasSize(retryCount);
  }
}

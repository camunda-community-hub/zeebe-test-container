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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.okhttp3.OkHttpClient;
import org.testcontainers.shaded.okhttp3.Request.Builder;
import org.testcontainers.shaded.okhttp3.Response;

class ZeebeBrokerNodeTest {
  @Timeout(value = 5, unit = TimeUnit.MINUTES)
  @ParameterizedTest(name = "{0} should be ready on start")
  @MethodSource("nodeProvider")
  void shouldBeReadyOnStart(final String testName, final ZeebeBrokerNode<?> node)
      throws IOException {
    // given
    final Response response;
    try (final GenericContainer<?> container = node.self()) {
      final OkHttpClient httpClient = new OkHttpClient();
      // when
      container.start();
      response =
          httpClient
              .newCall(
                  new Builder()
                      .get()
                      .url(String.format("http://%s/ready", node.getExternalMonitoringAddress()))
                      .build())
              .execute();
    }

    // then
    Assertions.assertThat(response.code()).isEqualTo(204);
  }

  @ParameterizedTest(name = "{0} should expose all ports except gateway")
  @MethodSource("nodeProvider")
  void shouldExposeAllPortsButGateway(final String testName, final ZeebeBrokerNode<?> node) {
    // given
    final List<Integer> expectedPorts =
        Arrays.stream(ZeebePort.values()).map(ZeebePort::getPort).collect(Collectors.toList());

    // when
    final List<Integer> exposedPorts = node.getExposedPorts();
    expectedPorts.remove((Integer) ZeebePort.GATEWAY.getPort());

    // then
    Assertions.assertThat(exposedPorts).containsAll(expectedPorts);
  }

  private static Stream<Arguments> nodeProvider() {
    final Stream<ZeebeBrokerNode<?>> nodes =
        Stream.of(new ZeebeContainer(), new ZeebeBrokerContainer());
    return nodes.map(node -> Arguments.arguments(node.getClass().getSimpleName(), node));
  }
}

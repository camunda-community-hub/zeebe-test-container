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

import java.util.HashMap;
import java.util.Map;
import org.agrona.collections.MutableInteger;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.Testcontainers;

@API(status = Status.INTERNAL)
public final class HostPortForwarder {
  private final PortForwarder portForwarder;

  public HostPortForwarder() {
    this(Testcontainers::exposeHostPorts);
  }

  public HostPortForwarder(final PortForwarder portForwarder) {
    this.portForwarder = portForwarder;
  }

  public static int forwardHostPort(final int port, final int retryCount) {
    return new HostPortForwarder().forward(port, retryCount);
  }

  /**
   * Exposes a given host port to your containers, accessible via host.testcontainers.internal:PORT.
   * See <a
   * href="https://www.testcontainers.org/features/networking/#exposing-host-ports-to-the-container">the
   * docs</a> for more.
   *
   * <p>This method is mostly here as a QoL improvement to retry on I/O errors.
   *
   * @param port the port on the host to expose
   * @param retryCount the number of times to retry on I/O errors
   * @return the container port to use
   */
  public int forward(final int port, final int retryCount) {
    final MutableInteger attempts = new MutableInteger();
    return Unreliables.retryUntilSuccess(
        retryCount,
        () -> {
          // since the port-forwarding requests are cached, use the attempt count to increment the
          // container port value, such that the request is always fresh on every retry
          final int containerPort = port + attempts.getAndIncrement();
          final Map<Integer, Integer> portMapping = new HashMap<>();
          portMapping.put(port, containerPort);

          portForwarder.forwardPort(portMapping);
          return containerPort;
        });
  }

  @FunctionalInterface
  public interface PortForwarder {
    void forwardPort(final Map<Integer, Integer> portMapping);
  }
}

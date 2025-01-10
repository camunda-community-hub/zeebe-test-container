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
package io.zeebe.containers.util;

import io.camunda.client.CamundaClient;
import io.zeebe.containers.ZeebeGatewayNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;

public final class TestSupport {
  private TestSupport() {}

  /**
   * Utility to get the current UID and GID such that a container can be run as that user.
   *
   * <p>NOTE: only works on Unix systems
   *
   * <p>This is especially useful if you need to mount a host file path with the right permissions.
   *
   * @return the current uid and gid as a string
   */
  public static String getRunAsUser() {
    return getUid() + ":" + getGid();
  }

  /**
   * NOTE: only works on Unix systems
   *
   * @return the current Unix group ID
   */
  public static String getGid() {
    return execCommand("id -g");
  }

  /**
   * NOTE: only works on Unix systems
   *
   * @return the current Unix user ID
   */
  public static String getUid() {
    return execCommand("id -u");
  }

  /** Returns a client for the given gateway, using a plaintext connection. */
  public static CamundaClient newZeebeClient(final ZeebeGatewayNode<?> gateway) {
    return CamundaClient.newClientBuilder()
        .usePlaintext()
        .grpcAddress(gateway.getGrpcAddress())
        .restAddress(gateway.getRestAddress())
        .build();
  }

  private static String execCommand(final String command) {
    try {
      final Process exec = Runtime.getRuntime().exec(command);
      final BufferedReader input = new BufferedReader(new InputStreamReader(exec.getInputStream()));
      return input.readLine();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}

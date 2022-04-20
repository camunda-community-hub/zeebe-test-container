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

import com.github.dockerjava.api.model.Info;
import io.camunda.zeebe.client.ZeebeClient;
import io.zeebe.containers.ZeebeGatewayNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.condition.DisabledIf;
import org.testcontainers.DockerClientFactory;

public final class TestUtils {
  private TestUtils() {}

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
  public static ZeebeClient newZeebeClient(final ZeebeGatewayNode<?> gateway) {
    return ZeebeClient.newClientBuilder()
        .usePlaintext()
        .gatewayAddress(gateway.getExternalGatewayAddress())
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

  /**
   * Returns whether tests run against Testcontainers Cloud or not by checking the server version.
   *
   * <p>This isn't perfect as it's clearly breaking implementation details, but it works for now.
   */
  public static boolean isCloudEnv() {
    if (System.getenv().containsKey("TC_CLOUD_TOKEN")) {
      return true;
    }

    final Info dockerInfo = DockerClientFactory.lazyClient().infoCmd().exec();
    return dockerInfo.getServerVersion() != null
        && dockerInfo.getServerVersion().endsWith("testcontainerscloud");
  }

  @DisabledIf(
      value = "io.zeebe.containers.util.TestUtils#isCloudEnv",
      disabledReason = "Testcontainers Cloud does not support mounting host files")
  @Target({ElementType.TYPE, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  public @interface DisabledIfTestcontainersCloud {}
}

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.testcontainers.containers.GenericContainer;

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

  /**
   * Returns the current {@link Instant} as seen by the container. The instant is truncated down to
   * millis, as nanoseconds are observed differently at times between the host and the container.
   *
   * @param container the container to inspect
   * @return the current instant from the container
   * @throws IOException if the container cannot be accessed
   * @throws InterruptedException if the thread is interrupted while awaiting the result of the
   *     command
   */
  public static Instant getContainerInstant(final GenericContainer<?> container)
      throws IOException, InterruptedException {
    final String output =
        container.execInContainer("/bin/date", "--utc", "+%s.%N").getStdout().trim();
    final String[] epoch = output.split("\\.", 2);

    return Instant.ofEpochSecond(Long.parseLong(epoch[0]), Long.parseLong(epoch[1]))
        .truncatedTo(ChronoUnit.MILLIS);
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

/*
 * Copyright © 2021 camunda services GmbH (info@camunda.com)
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

import java.time.Duration;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.testcontainers.containers.GenericContainer;

/**
 * To use, create a new instance with the desired port (or, by default, 5005), and pass the
 * container you wish to configure to it.
 *
 * <p>This will start the container and wait for the debugger to attach. To use with Intellij,
 * create a new debug configuration template and pick "Remote JVM Debug". By default, it will be
 * setup for port 5005, which is the default port here as well. You can find out more about this
 * from <a href="https://www.jetbrains.com/help/idea/tutorial-remote-debug.html">this IntelliJ
 * tutorial</a>.
 *
 * <p>This idea came from bsideup's blog, and you can read more about it <a
 * href="https://bsideup.github.io/posts/debugging_containers/">here</a>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * final ZeebeContainer container = new ZeebeContainer();
 * RemoteDebugger.configure(container, 5005, true);
 * }</pre>
 *
 * <pre>{@code
 * @Testcontainers
 * final class MyTest {
 *   @Container
 *   private final ZeebeContainer = RemoteDebugger.configure(new ZeebeContainer())
 *       .withEnv("MY_ENV_VAR", "true");
 *
 *   // more test code...
 * }
 * }</pre>
 */
@API(status = Status.EXPERIMENTAL)
public final class RemoteDebugger {

  /** The default binding port for the remote debugger server */
  public static final int DEFAULT_REMOTE_DEBUGGER_PORT = 5005;

  /** The default timeout that will be applied to the container being debugged */
  public static final Duration DEFAULT_START_TIMEOUT = Duration.ofMinutes(5);

  private RemoteDebugger() {}

  /**
   * Returns the given container configured to start a debugging server. Uses the default port 5005.
   * By default, the application will be suspended until a debugger connects to it.
   *
   * @param container the container to configure
   * @param <T> the type of the container
   * @return the same container configured for debugging
   */
  public static <T extends GenericContainer<T>> T configure(final T container) {
    return configure(container, DEFAULT_REMOTE_DEBUGGER_PORT);
  }

  /**
   * Returns the given container configured to start a debugging server on the given port. By
   * default, the application will be suspended until a debugger connects to it.
   *
   * @param container the container to configure
   * @param port the port to bind the debugging server to
   * @param <T> the type of the container
   * @return the same container configured for debugging
   */
  public static <T extends GenericContainer<T>> T configure(final T container, final int port) {
    return configure(container, port, true);
  }

  /**
   * Returns the given container configured to start a debugging server on the given port. If {@code
   * suspend} is true, the application is suspended until a debugger instance connects to the
   * server.
   *
   * @param container the container to configure
   * @param port the port to bind the debugging server to
   * @param suspend if true, will suspend the application until a debugger connects to it
   * @param <T> the type of the container
   * @return the same container configured for debugging
   */
  public static <T extends GenericContainer<T>> T configure(
      final T container, final int port, boolean suspend) {
    final String javaOpts = container.getEnvMap().getOrDefault("JAVA_OPTS", "");
    final char suspendFlag = suspend ? 'y' : 'n';

    // when configuring a port binding, we need to expose the port as well; the port binding just
    // decides to which host port the exposed port will bind, but it will not expose the port itself
    container.addExposedPort(port);
    container.getPortBindings().add(port + ":" + port);

    // prepend agent configuration in front of javaOpts to ensure it's enabled but also keep
    // previously defined options
    container.withEnv(
        "JAVA_OPTS",
        String.format(
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=%s,address=0.0.0.0:%d %s",
            suspendFlag, port, javaOpts));

    return container.withStartupTimeout(DEFAULT_START_TIMEOUT);
  }
}

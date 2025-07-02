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

import java.time.Duration;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy.Mode;
import org.testcontainers.utility.DockerImageName;

/**
 * Represents a Zeebe broker docker instance - that is, without an embedded gateway. By default, it
 * will expose all {@link ZeebePort} ports except the gateway port.
 *
 * <p>It is considered ready if:
 *
 * <ul>
 *   <li>its ports are ready (see {@link HostPortWaitStrategy})
 *   <li>the ready check is successful (see {@link #newDefaultBrokerReadyCheck()})
 * </ul>
 *
 * <h2>Connecting to other nodes</h2>
 *
 * <p>If you want to connect this broker to other nodes, the recommended way is to create a new
 * network (e.g. {@link Network#newNetwork()}) and set it as the network of each container you wish
 * to connect together via {@link GenericContainer#setNetwork(Network)}. Furthermore, you have to
 * make sure that all nodes share the same cluster name (see Zeebe documentation on how to configure
 * that).
 *
 * <p>You can connect other brokers to it by adding this container's {@link
 * #getInternalClusterAddress()} to their {@code initialContactPoints} list. Note that this has to
 * be done before starting the containers.
 *
 * <p>You can connect a standalone gateway by setting its {@code contactPoint} to this container's
 * {@link #getInternalClusterAddress()}.
 *
 * <p>If you want to reuse the same data across restarts, you can specify it using {@link
 * ZeebeBrokerNode#withZeebeData(ZeebeData)}.
 */
@API(status = Status.STABLE)
@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "java:S2160"})
public class ZeebeBrokerContainer extends GenericContainer<ZeebeBrokerContainer>
    implements ZeebeBrokerNode<ZeebeBrokerContainer> {

  private static final Duration DEFAULT_STARTUP_TIMEOUT = Duration.ofMinutes(1);

  /**
   * Creates a new container with the default Zeebe image and version.
   *
   * @see ZeebeDefaults#getDefaultImage()
   * @see ZeebeDefaults#getDefaultVersion()
   */
  public ZeebeBrokerContainer() {
    this(ZeebeDefaults.getInstance().getDefaultDockerImage());
  }

  /**
   * @param dockerImageName the full docker image name
   */
  public ZeebeBrokerContainer(final DockerImageName dockerImageName) {
    super(dockerImageName);
    applyDefaultConfiguration();
  }

  /**
   * Creates a new {@link HttpWaitStrategy} configured for the default broker ready check. Available
   * publicly to be modified as desired.
   *
   * @return the default broker ready check
   */
  public static HttpWaitStrategy newDefaultBrokerReadyCheck() {
    return new HttpWaitStrategy()
        .forPath("/ready")
        .forPort(ZeebePort.MONITORING.getPort())
        .forStatusCodeMatching(status -> status >= 200 && status < 300)
        .withReadTimeout(Duration.ofSeconds(10));
  }

  private WaitAllStrategy newDefaultWaitStrategy() {
    return new WaitAllStrategy(Mode.WITH_OUTER_TIMEOUT)
        .withStrategy(new HostPortWaitStrategy())
        .withStrategy(newDefaultBrokerReadyCheck())
        .withStartupTimeout(DEFAULT_STARTUP_TIMEOUT);
  }

  private void applyDefaultConfiguration() {
    withNetwork(Network.SHARED)
        .waitingFor(newDefaultWaitStrategy())
        .withEnv("ZEEBE_BROKER_GATEWAY_ENABLE", "false")
        .withEnv("ZEEBE_BROKER_NETWORK_HOST", "0.0.0.0")
        .withEnv("ZEEBE_BROKER_NETWORK_ADVERTISEDHOST", getInternalHost())
        .withEnv("CAMUNDA_SECURITY_AUTHENTICATION_UNPROTECTEDAPI", "true")
        .withEnv("CAMUNDA_SECURITY_AUTHORIZATIONS_ENABLED", "false")
        .addExposedPorts(
            ZeebePort.COMMAND.getPort(),
            ZeebePort.INTERNAL.getPort(),
            ZeebePort.MONITORING.getPort());
  }
}

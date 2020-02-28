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
package io.zeebe.containers.impl;

import com.github.dockerjava.api.command.InspectContainerResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

public class BrokerWaitStrategy implements WaitStrategy {

  private final WaitStrategy readyCheck;
  private final WaitStrategy commandApiCheck;

  public BrokerWaitStrategy() {
    readyCheck =
        new HttpWaitStrategy()
            .forPath("/ready")
            .forPort(ZeebePort.MONITORING_API.getPort())
            .forStatusCode(204)
            .withReadTimeout(Duration.ofSeconds(10));
    commandApiCheck = new HostPortWaitStrategy();
  }

  @Override
  public void waitUntilReady(WaitStrategyTarget waitStrategyTarget) {
    readyCheck.waitUntilReady(waitStrategyTarget);
    commandApiCheck.waitUntilReady(new BrokerWaitStrategyTarget(waitStrategyTarget));
  }

  @Override
  public WaitStrategy withStartupTimeout(Duration startupTimeout) {
    readyCheck.withStartupTimeout(startupTimeout);
    commandApiCheck.withStartupTimeout(startupTimeout);
    return this;
  }

  private static final class BrokerWaitStrategyTarget implements WaitStrategyTarget {

    private final WaitStrategyTarget waitStrategyTarget;

    public BrokerWaitStrategyTarget(WaitStrategyTarget waitStrategyTarget) {
      this.waitStrategyTarget = waitStrategyTarget;
    }

    @Override
    public List<Integer> getExposedPorts() {
      return waitStrategyTarget.getExposedPorts();
    }

    @Override
    public InspectContainerResponse getContainerInfo() {
      return waitStrategyTarget.getContainerInfo();
    }

    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
      return Collections.singleton(
          waitStrategyTarget.getMappedPort(ZeebePort.COMMAND_API.getPort()));
    }
  }
}

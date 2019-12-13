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

import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.response.BrokerInfo;
import io.zeebe.client.api.response.Topology;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import org.rnorth.ducttape.unreliables.Unreliables;

/**
 * Provides a collection of synchronous methods to wait until the system reaches a certain state.
 */
public final class Awaitables {
  static Topology awaitTopology(
      final ZeebeClient client, final int expectedBrokersCount, final int expectedPartitionsCount) {
    return Unreliables.retryUntilSuccess(
        5,
        TimeUnit.SECONDS,
        () -> getTopology(client, expectedBrokersCount, expectedPartitionsCount));
  }

  private static Topology getTopology(
      final ZeebeClient client, final int expectedBrokersCount, final int expectedPartitionsCount) {
    final Topology topology = client.newTopologyRequest().send().join();
    final List<BrokerInfo> brokers = topology.getBrokers();

    if (brokers.size() == expectedBrokersCount) {
      if (brokers.stream().allMatch(b -> b.getPartitions().size() == expectedPartitionsCount)) {
        return topology;
      }
    }

    throw new NoSuchElementException(
        String.format(
            "Expected topology to contain %d brokers with %d partitions, but got %s",
            expectedBrokersCount, expectedPartitionsCount, topology.toString()));
  }
}

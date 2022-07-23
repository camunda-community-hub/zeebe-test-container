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
package io.zeebe.containers.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.Thread.State;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class InfiniteListTest {
  @Test
  void shouldReturnSize() {
    // given
    final InfiniteList<Integer> list = new InfiniteList<>();
    list.add(1);

    // when
    final int initialSize = list.size();
    list.add(2);
    final int finalSize = list.size();

    // then
    assertThat(initialSize).isEqualTo(1);
    assertThat(finalSize).isEqualTo(2);
  }

  @Nested
  final class IterationTest {
    @Test
    void shouldTailItems() {
      // given
      final InfiniteList<Integer> list = new InfiniteList<>();
      final Iterator<Integer> iterator = list.iterator();

      // when
      list.add(1);

      // then
      assertThat(iterator).hasNext();
      assertThat(iterator.next()).isEqualTo(1);
    }

    @Test
    void shouldSignalOnNewItems() throws InterruptedException {
      // given
      final InfiniteList<Integer> list = new InfiniteList<>(Duration.ofMinutes(5));
      final Iterator<Integer> iterator = list.iterator();
      final AtomicReference<Integer> receivedItem = new AtomicReference<>();

      // when
      final Thread thread = new Thread(() -> receivedItem.set(iterator.next()));
      thread.start();

      Awaitility.await("until the thread is parked and waiting for the next item")
          .atMost(Duration.ofSeconds(10))
          .untilAsserted(() -> assertThat(thread.getState()).isEqualTo(State.TIMED_WAITING));
      list.add(1);
      thread.join();

      // then
      assertThat(receivedItem).hasValue(1);
    }

    @Test
    void shouldTimeoutWhenNoItems() throws InterruptedException {
      // given
      final InfiniteList<Integer> list = new InfiniteList<>(Duration.ofSeconds(1));
      final Iterator<Integer> iterator = list.iterator();
      final AtomicBoolean hasNext = new AtomicBoolean(true);

      // when
      final Thread thread = new Thread(() -> hasNext.set(iterator.hasNext()));
      final Instant start = Instant.now();
      thread.start();

      Awaitility.await("until the thread is parked and waiting for the next item")
          .atMost(Duration.ofSeconds(10))
          .untilAsserted(() -> assertThat(thread.getState()).isEqualTo(State.TIMED_WAITING));
      thread.join();
      final Instant finish = Instant.now();

      // then - give some lenient timeout, especially the upper bound
      assertThat(finish).isBetween(start.plusMillis(800), start.plusMillis(2000));
      assertThat(hasNext).isFalse();
    }
  }
}

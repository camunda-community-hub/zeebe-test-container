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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import net.jcip.annotations.ThreadSafe;
import org.agrona.LangUtil;

/**
 * An {@link Iterator} implementation which tails a list, meaning it will see items added after the
 * iterator was created, but before it reached the end of the list. It can optionally be configured
 * to have a grace period to wait for new items once it reaches its end. If a new item comes in at
 * that point, the grace period is reset.
 *
 * @param <T> the type of the items iterated on
 */
@ThreadSafe
final class InfiniteIterator<T> implements Iterator<T> {
  private final List<T> items;
  private final Duration timeout;
  private int cursor = 0;

  /**
   * Creates a new iterator which will tail the given collection, optionally waiting for new items
   * if it reaches the end.
   *
   * <p>To disable waiting at the end, simply pass a negative or zero duration.
   *
   * @param items the items to tail
   * @param timeout the optional timeout to wait for when reaching the end of the list
   */
  InfiniteIterator(final List<T> items, final Duration timeout) {
    this.items = items;
    this.timeout = timeout;
  }

  @Override
  public boolean hasNext() {
    synchronized (items) {
      try {
        Duration currentTimeout = timeout;

        while (hasTimeRemaining(currentTimeout) && cursor >= items.size()) {
          final long currentNanos = System.nanoTime();
          items.wait(currentTimeout.toMillis(), currentTimeout.getNano());
          currentTimeout = Duration.of(currentNanos - System.nanoTime(), ChronoUnit.NANOS);
        }

        return cursor < items.size();
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        LangUtil.rethrowUnchecked(e);
      }
    }

    return false;
  }

  @Override
  public T next() {
    final T item;

    synchronized (items) {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      item = items.get(cursor);
      cursor++;
    }

    return item;
  }

  private boolean hasTimeRemaining(final Duration duration) {
    return !duration.isNegative() && !duration.isZero();
  }
}

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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import net.jcip.annotations.ThreadSafe;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/**
 * A thread safe wrapper around a list which will produce {@link InfiniteIterator} instances for
 * said list.
 *
 * @param <T> the type of the list entries
 */
@ThreadSafe
@API(status = Status.INTERNAL)
final class InfiniteList<T> implements Iterable<T> {
  private final Duration timeout;
  private final List<T> items;

  InfiniteList() {
    this(Duration.ZERO);
  }

  InfiniteList(final Duration timeout) {
    this.timeout = timeout;
    items = new LinkedList<>();
  }

  @Override
  public Iterator<T> iterator() {
    return new InfiniteIterator<>(items, timeout);
  }

  int size() {
    return items.size();
  }

  void add(final T item) {
    synchronized (items) {
      items.add(item);
      items.notifyAll();
    }
  }
}

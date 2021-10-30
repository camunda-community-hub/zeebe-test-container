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
package io.zeebe.containers.clock;

import java.time.Duration;
import java.time.Instant;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/**
 * Implementations of this interface can change the time of a container, either by adding or
 * subtracting offsets, or pinning the time to a specific instant.
 */
@API(status = Status.INTERNAL)
public interface ContainerClock {

  /**
   * Adds a relative offset to the current time.
   *
   * <p>If the time is currently pinned, it will effectively add that offset to the pinned time, but
   * will not unpin it - that is, time will remain frozen.
   *
   * <p>If the time is not pinned, then this will add/subtract the offset to the current time, and
   * time will keep moving.
   *
   * <p>Calling this method multiple times is cumulative, i.e. the offset is not reset on every
   * call.
   *
   * <p>To travel back in time, pass a negative duration.
   *
   * @param offset the amount of time to add to the current time
   */
  void addTime(final Duration offset);

  /**
   * Pins the current time to the given instant. Any subsequent calls to something like {@link
   * System#currentTimeMillis()} in the container will return this instant.
   *
   * @param time the time to fix
   */
  void pinTime(final Instant time);

  /**
   * Unpins the pinned time. This will cause time to start moving forward relatively from the
   * previously pinned instant. However, note that adding time will
   *
   * <p>If the time was not previously pinned, this method does nothing.
   *
   * <p>NOTE: unpinning is not perfectly accurate, but it should be relatively accurate to within a
   * second.
   */
  void unpinTime();

  /** Resets the time to whatever the real system time is. */
  void resetTime();
}

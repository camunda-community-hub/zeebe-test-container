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

import feign.Target.HardCodedTarget;
import io.zeebe.containers.ZeebeNode;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/**
 * A high level API to interact with a node's actor clock.
 *
 * <p>You can use this to more easily test time based processes, such as triggering a broker to take
 * a snapshot, triggering a timeout on the gateway, or triggering a timer in a deployed process.
 *
 * <p>Be careful as changing the actor clock can trigger multiple things to occur at the same time.
 * Even though you only want to trigger your BPMN process's timer event, changing the clock can
 * trigger a Raft election as a side effect, causing temporary unavailability and introducing
 * uncertainty in your tests. When using this, try to ensure that the scope of the test (both in
 * terms of setup and assertions) is as narrow as possible.
 *
 * <p>NOTE: this is only compatible with Zeebe versions 1.3.x and above.
 */
@SuppressWarnings("unused")
@API(status = Status.EXPERIMENTAL)
public interface ZeebeClock {

  /**
   * Pins the clock to the given instant, after which time will stop ticking. You can use {@link
   * #addTime(Duration)} to rewind/advance time after this. Using either of these methods will not
   * however "unfreeze" time. To do so, you need to call {@link #resetTime()}.
   *
   * @param time the time to pin the clock at
   * @return the clock's current time after pinning (which should be the given {@code time})
   * @throws ZeebeClockException if any error occur; inspect the cause to know more
   */
  Instant pinTime(final Instant time);

  /**
   * Adds the given offset to the current clock. To rewind time, pass a negative duration. If the
   * clock is pinned, the offset is added to/subtracted from the pinned time, but time is remains
   * frozen.
   *
   * @param offset the offset to add to/subtract from the clock's current time
   * @return the clock's current time after adding the offset
   * @throws ZeebeClockException if any error occur; inspect the cause to know more
   */
  Instant addTime(final Duration offset);

  /**
   * Returns the clock's current time
   *
   * @throws ZeebeClockException if any error occur; inspect the cause to know more
   */
  Instant getCurrentTime();

  /**
   * Resets the time to the actual system time of the node. This will unpin and unfreeze time if the
   * clock was previously pinned, and will also remove any offsets.
   *
   * @return the clock's current time after reset
   * @throws ZeebeClockException if any error occur; inspect the cause to know more
   */
  Instant resetTime();

  /**
   * Factory method for the default clock implementation, targeting the given {@link ZeebeNode}.
   *
   * @param zeebe the Zeebe node to interact with
   * @return a default implementation of the clock targeting the given node
   */
  static ZeebeClock newDefaultClock(final ZeebeNode<?> zeebe) {
    return newDefaultClock(zeebe, "http");
  }

  /**
   * Factory method for the default clock implementation, targeting the given {@link ZeebeNode}.
   *
   * @param zeebe the Zeebe node to interact with
   * @param scheme the URL scheme to use, e.g. http or https
   * @return a default implementation of the clock targeting the given node
   */
  static ZeebeClock newDefaultClock(final ZeebeNode<?> zeebe, final String scheme) {
    return new ZeebeClockImpl(new ZeebeClockTarget(zeebe, scheme));
  }

  /**
   * Factory method for the default clock implementation, targeting some arbitrary URL.
   *
   * @param url the actuator clock endpoint URL
   * @return a default implementation of the clock targeting the given URL
   */
  static ZeebeClock newDefaultClock(final URL url) {
    return new ZeebeClockImpl(new HardCodedTarget<>(ZeebeClockClient.class, url.toString()));
  }

  /**
   * Every clock operation can throw this exception. Most of the time, it will simply act as a
   * wrapper for more specific exceptions. However, it is useful to provide users a way to handle
   * errors that occurred specifically when interacting with the clock as opposed to more generic
   * exceptions. It also provides a unified way to handle errors with multiple implementations of
   * the interface.
   */
  final class ZeebeClockException extends RuntimeException {
    public ZeebeClockException(final Throwable cause) {
      super(cause);
    }
  }
}

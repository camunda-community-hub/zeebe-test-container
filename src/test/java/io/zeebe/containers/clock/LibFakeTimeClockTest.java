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

import static org.assertj.core.api.Assertions.assertThat;

import io.zeebe.containers.util.TestUtils;
import io.zeebe.containers.util.TinyContainer;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.utility.DockerImageName;

/**
 * NOTE: timestamps are compared down with a small variance, as I've observed differences of a few
 * nanoseconds at times, which I attribute to rounding errors when parsing the date on the container
 * side or on this side. While I can't be sure, having a very small variance is not a blocker in
 * this case, you can always truncate the date down to micros or millis, e.g. {@link
 * Instant#truncatedTo(TemporalUnit)}, and still get value from this. In most cases this is accurate
 * enough.
 *
 * <p>The tests also use the smallest available Debian based image, as an Alpine image (the default
 * for {@link TinyContainer}) will not be able to load the packaged libfaketime.
 */
@Execution(ExecutionMode.CONCURRENT)
@SuppressWarnings("java:S2925")
final class LibFakeTimeClockTest {
  private static final DockerImageName IMAGE_NAME =
      DockerImageName.parse("bitnami/minideb:bullseye-amd64");

  @Test
  void shouldPinTime() throws IOException, InterruptedException {
    // given
    final LibFakeTimeClock clock = LibFakeTimeClock.withTempFile();
    final Instant pinnedTime = Instant.now().minus(Duration.ofDays(1));
    final Instant containerTime;

    // when
    try (final TinyContainer container = new TinyContainer(IMAGE_NAME)) {
      clock.configure(container);
      clock.pinTime(pinnedTime);
      container.start();

      Thread.sleep(500);
      containerTime = TestUtils.getContainerInstant(container);
    }

    // then
    assertThat(containerTime).isBetween(pinnedTime.minusMillis(10), pinnedTime.plusMillis(10));
  }

  @Test
  void shouldUnpinTime() throws IOException, InterruptedException {
    // given
    final LibFakeTimeClock clock = LibFakeTimeClock.withTempFile();
    final Instant pinnedTime = Instant.now().minus(Duration.ofDays(1));
    final Instant containerTime;

    // when
    try (final TinyContainer container = new TinyContainer(IMAGE_NAME)) {
      clock.configure(container);
      clock.pinTime(pinnedTime);
      container.start();

      clock.unpinTime();
      Thread.sleep(500);
      containerTime = TestUtils.getContainerInstant(container);
    }

    // then
    // unfortunately we have to give a generous upper bound to avoid flakiness
    assertThat(containerTime).isBetween(pinnedTime.minusMillis(10), pinnedTime.plusSeconds(5));
  }

  @Test
  void shouldResetPinnedTime() throws IOException, InterruptedException {
    // given
    final LibFakeTimeClock clock = LibFakeTimeClock.withTempFile();
    final Instant now = Instant.now();
    final Instant pinnedTime = now.minus(Duration.ofDays(1));
    final Instant containerTime;

    // when
    try (final TinyContainer container = new TinyContainer(IMAGE_NAME)) {
      clock.configure(container);
      clock.pinTime(pinnedTime);
      container.start();
      clock.resetTime();
      containerTime = TestUtils.getContainerInstant(container);
    }

    // then
    // unfortunately we have to give a generous upper bound to avoid flakiness
    assertThat(containerTime).isBetween(now.minusMillis(10), now.plusSeconds(30));
  }

  @Test
  void shouldResetAddedTime() throws IOException, InterruptedException {
    // given
    final LibFakeTimeClock clock = LibFakeTimeClock.withTempFile();
    final Instant now = Instant.now();
    final Instant containerTime;

    // when
    try (final TinyContainer container = new TinyContainer(IMAGE_NAME)) {
      clock.configure(container);
      container.start();
      clock.addTime(Duration.ofMinutes(10));
      clock.resetTime();
      containerTime = TestUtils.getContainerInstant(container);
    }

    // then
    // unfortunately we have to give a generous upper bound to avoid flakiness
    assertThat(containerTime).isBetween(now.minusMillis(10), now.plusSeconds(30));
  }

  @Test
  void shouldNotAddTimeCumulativelyAfterReset() throws IOException, InterruptedException {
    // given
    final LibFakeTimeClock clock = LibFakeTimeClock.withTempFile();
    final Instant now = Instant.now();
    final Instant containerTime;

    // when
    try (final TinyContainer container = new TinyContainer(IMAGE_NAME)) {
      clock.configure(container);
      container.start();
      clock.addTime(Duration.ofMinutes(10));
      clock.resetTime();
      clock.addTime(Duration.ofMinutes(10));
      containerTime = TestUtils.getContainerInstant(container);
    }

    // then
    // unfortunately we have to give a generous upper bound to avoid flakiness
    assertThat(containerTime)
        .isBetween(
            now.plus(Duration.ofMinutes(10)).minusMillis(10), now.plus(Duration.ofMinutes(11)));
  }

  @Test
  void shouldAddTime() throws IOException, InterruptedException {
    // given
    final LibFakeTimeClock clock = LibFakeTimeClock.withTempFile();
    final Instant now = Instant.now();
    final Duration offset = Duration.ofSeconds(60);
    final Instant containerTime;

    // when
    try (final TinyContainer container = new TinyContainer(IMAGE_NAME)) {
      clock.configure(container);
      clock.addTime(offset);
      container.start();
      containerTime = TestUtils.getContainerInstant(container);
    }

    // then
    // unfortunately we have to give a generous upper bound to avoid flakiness
    assertThat(containerTime)
        .isBetween(now.plus(offset).minusMillis(10), now.plus(Duration.ofSeconds(90)));
  }

  @Test
  void shouldAddTimeCumulatively() throws IOException, InterruptedException {
    // given
    final LibFakeTimeClock clock = LibFakeTimeClock.withTempFile();
    final Instant now = Instant.now();
    final Duration offset = Duration.ofSeconds(60);
    final Instant containerTime;

    // when
    try (final TinyContainer container = new TinyContainer(IMAGE_NAME)) {
      clock.configure(container);
      clock.addTime(offset);
      container.start();
      clock.addTime(offset);
      containerTime = TestUtils.getContainerInstant(container);
    }

    // then
    // unfortunately we have to give a generous upper bound to avoid flakiness
    assertThat(containerTime)
        .isBetween(
            now.plus(offset).plus(offset).minusMillis(10), now.plus(Duration.ofSeconds(150)));
  }

  @Test
  void shouldAddTimeToPinnedTime() throws IOException, InterruptedException {
    // given
    final LibFakeTimeClock clock = LibFakeTimeClock.withTempFile();
    final Instant pinnedTime = Instant.now();
    final Duration offset = Duration.ofSeconds(60);
    final Instant containerTime;

    // when
    try (final TinyContainer container = new TinyContainer(IMAGE_NAME)) {
      clock.configure(container);
      clock.pinTime(pinnedTime);
      container.start();
      clock.addTime(offset);
      containerTime = TestUtils.getContainerInstant(container);
    }

    // then
    // unfortunately we have to give an inaccurate bounds to avoid flakiness
    assertThat(containerTime)
        .isBetween(
            pinnedTime.plus(offset).minusMillis(10), pinnedTime.plus(Duration.ofSeconds(90)));
  }

  @Test
  void shouldResetOffsetWhenPinningTime() throws IOException, InterruptedException {
    // given
    final LibFakeTimeClock clock = LibFakeTimeClock.withTempFile();
    final Instant pinnedTime = Instant.now();
    final Duration offset = Duration.ofSeconds(60);
    final Instant containerTime;

    // when
    try (final TinyContainer container = new TinyContainer(IMAGE_NAME)) {
      clock.configure(container);
      clock.addTime(offset);
      container.start();
      clock.pinTime(pinnedTime);
      clock.addTime(offset);
      containerTime = TestUtils.getContainerInstant(container);
    }

    // then
    // unfortunately we have to give an inaccurate bounds to avoid flakiness
    assertThat(containerTime)
        .isBetween(
            pinnedTime.plus(offset).minusMillis(10), pinnedTime.plus(Duration.ofSeconds(90)));
  }
}

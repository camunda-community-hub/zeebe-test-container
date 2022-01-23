package io.zeebe.containers.clock;

import static org.assertj.core.api.Assertions.assertThat;

import io.zeebe.containers.ZeebeBrokerContainer;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Contract testing for any {@link ZeebeClock} implementations.
 *
 * <p>NOTE: if there is ever more than one implementation, please turn the tests into parameterized
 * tests and run the suite for each implementation.
 *
 * <p>NOTE: tests are run sequentially to allow reuse of the same backend
 */
@Execution(ExecutionMode.SAME_THREAD)
@Testcontainers
final class ZeebeClockTest {
  @Container
  private static final ZeebeBrokerContainer BROKER =
      new ZeebeBrokerContainer().withEnv("ZEEBE_CLOCK_CONTROLLED", "true");

  private final ZeebeClock clock = ZeebeClock.newDefaultClock(BROKER);

  @AfterEach
  void afterEach() {
    ZeebeClock.newDefaultClock(BROKER).resetTime();
  }

  @Test
  void shouldGetCurrentTime() {
    // given
    final Instant pinnedTime = clock.pinTime(Instant.now());

    // when
    final Instant currentTime = clock.getCurrentTime();

    // then
    assertThat(currentTime).isEqualTo(pinnedTime);
  }

  @Test
  void shouldPinTime() {
    // given
    final Instant previousTime = clock.getCurrentTime();
    final Instant expectedPinnedTime = previousTime.plusSeconds(10);

    // when
    final Instant actualPinnedTime = clock.pinTime(expectedPinnedTime);

    // then
    assertThat(actualPinnedTime).isAfter(previousTime).isEqualTo(expectedPinnedTime);
  }

  @Test
  void shouldAddTime() {
    // given
    final Instant pinnedTime = clock.pinTime(Instant.now());
    final Duration offset = Duration.ofDays(1);

    // when
    final Instant modifiedTime = clock.addTime(offset);

    // then
    final Instant expectedTime = pinnedTime.plus(offset);
    assertThat(modifiedTime).isEqualTo(pinnedTime.plus(offset)).isEqualTo(expectedTime);
  }

  @Test
  void shouldResetTime() {
    // given
    final Instant pinnedTime = clock.pinTime(Instant.now());
    final Duration offset = Duration.ofDays(1);

    // when
    final Instant modifiedTime = clock.addTime(offset);
    final Instant resetTime = clock.resetTime();

    // then
    assertThat(resetTime).isBefore(modifiedTime).isAfter(pinnedTime);
  }
}

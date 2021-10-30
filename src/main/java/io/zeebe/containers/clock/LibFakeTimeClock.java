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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

/**
 * An implementation of {@link ContainerClock} which uses <a
 * href="https://github.com/wolfcw/libfaketime">libfaketime</a> to control the time on the
 * container.
 *
 * <p>The packaged library was built in the Zeebe container, and is not portable, i.e. if you use a
 * custom image it may not work.
 *
 * <p>Using this implementation will configure (via {@link #configure(GenericContainer)} the
 * container to preload a copied `libfaketime.so.1` (copied from the classpath) for every program in
 * the container by setting the environment variable `LD_PRELOAD`. This includes any interactive
 * commands attaching themselves to the container, for example.
 *
 * <p>Setting the time is done via a shared file; writing to that file will update the time in the
 * container.
 */
@API(status = Status.INTERNAL)
public final class LibFakeTimeClock implements ContainerClock {
  private static final String NULL_OFFSET = "+0.000";
  private static final String PRELOAD_ENV_VAR = "LD_PRELOAD";
  private static final MountableFile LIB_FAKETIME =
      MountableFile.forClasspathResource("libfaketime.so.1");
  private static final String LIB_FAKETIME_MOUNT = "/tmp/libfaketime.so.1";
  private static final String FAKETIME_FILE_MOUNT = "/tmp/faketime.rc";
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss").withZone(ZoneOffset.UTC);
  private static final DecimalFormat DURATION_FORMATTER = new DecimalFormat();

  static {
    DURATION_FORMATTER.setMinimumIntegerDigits(1);
    DURATION_FORMATTER.setMinimumFractionDigits(3);
    DURATION_FORMATTER.setMaximumFractionDigits(3);
    DURATION_FORMATTER.setPositivePrefix("+");
    DURATION_FORMATTER.setNegativePrefix("-");
  }

  private final Path sharedFile;

  private Instant currentTime;
  private Duration currentOffset;

  LibFakeTimeClock(final Path sharedFile) {
    this.sharedFile = sharedFile;
  }

  /**
   * Creates a temporary file via {@link Files#createTempFile(String, String, FileAttribute[])} with
   * the prefix {@code faketime} and the suffix {@code rc}.
   *
   * @return a {@link LibFakeTimeClock} using a temporary file
   */
  public static LibFakeTimeClock withTempFile() {
    try {
      final Path tempFile = Files.createTempFile("faketime", ".rc");
      return new LibFakeTimeClock(tempFile);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void addTime(final Duration offset) {
    if (isTimePinned()) {
      currentTime = currentTime.plus(offset);
      pinTime(currentTime);
    } else {
      if (currentOffset == null) {
        currentOffset = offset;
      } else {
        currentOffset = currentOffset.plus(offset);
      }

      updateTime(formatTime(currentOffset));
    }
  }

  @Override
  public void pinTime(final Instant time) {
    this.currentTime = time;
    updateTime(formatTime(time));
  }

  @Override
  public void unpinTime() {
    if (currentTime == null) {
      return;
    }

    final Instant now = Instant.now();
    final Duration offset = Duration.between(now, currentTime);
    LoggerFactory.getLogger(LibFakeTimeClock.class)
        .info("Time elapsed between {} and {}: {}", now, currentTime, offset);

    currentOffset = offset;
    updateTime(formatTime(currentOffset));
  }

  @Override
  public void resetTime() {
    updateTime(NULL_OFFSET);
  }

  private String formatTime(final Instant instant) {
    return DATE_TIME_FORMATTER.format(instant);
  }

  private String formatTime(final Duration offset) {
    return DURATION_FORMATTER.format(offset.toMillis() / 1000);
  }

  private void updateTime(final String time) {
    final byte[] bytes = time.getBytes(StandardCharsets.UTF_8);
    final ByteBuffer bufferView = ByteBuffer.wrap(bytes);

    try (final FileChannel channel =
        FileChannel.open(
            sharedFile,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.SYNC)) {
      channel.truncate(0);
      channel.write(bufferView);
    } catch (final IOException e) {
      throw new UncheckedIOException(
          String.format(
              "Expected to update the container clock to '%s', but failed to write to '%s'",
              time, sharedFile),
          e);
    }
  }

  private boolean isTimePinned() {
    return currentTime != null;
  }

  public <T extends GenericContainer<T>> void configure(final T container) {
    final String ldPreload = container.getEnvMap().get(PRELOAD_ENV_VAR);
    container
        .withCopyFileToContainer(LIB_FAKETIME, LIB_FAKETIME_MOUNT)
        .withFileSystemBind(
            sharedFile.toAbsolutePath().toString(), FAKETIME_FILE_MOUNT, BindMode.READ_WRITE);

    if (ldPreload == null || ldPreload.isEmpty()) {
      container.withEnv(PRELOAD_ENV_VAR, LIB_FAKETIME_MOUNT);
    } else {
      container.withEnv(PRELOAD_ENV_VAR, ldPreload + " " + LIB_FAKETIME_MOUNT);
    }

    container
        .withEnv("FAKETIME_TIMESTAMP_FILE", FAKETIME_FILE_MOUNT)
        .withEnv("FAKETIME_DONT_FAKE_MONOTONIC", "1") // ensure System.nanoTime() will work properly
        .withEnv("FAKETIME_DONT_RESET", "1"); // allows us to "unpin" time

    // having an empty file will cause issues, so better reset with some 0 offset
    resetTime();
  }
}

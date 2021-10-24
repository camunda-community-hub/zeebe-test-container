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
package io.zeebe.containers.util;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.slf4j.Logger;

/**
 * Implements a {@link ResultCallback} to be used with {@link
 * com.github.dockerjava.api.command.ExecStartCmd}, allowing consumers to wait for the result of
 * executing a command on the container. If you want the logs to be piped to the given logger, be
 * sure to also attach an STDOUT and STDERR when creating the command, e.g. {@link
 * com.github.dockerjava.api.command.ExecCreateCmd#withAttachStdout(Boolean)}.
 *
 * <p>NOTE: this class takes in a logger to better identify logs coming in for the different
 * commands, so make sure to pass in an appropriate logger.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * final DockerClient client = DockerClientFactory.lazyClient();
 * final String execCommandId =
 *     client
 *         .execCreateCmd(containerId)
 *         .withCmd("tar", "-chzf", archivePath, containerPath)
 *         .withAttachStdout(true)
 *         .withAttachStderr(true)
 *         .exec()
 *         .getId();
 * final SynchronousDockerExecHandler handler = new SynchronousDockerExecHandler(LOGGER);
 * client.execStartCmd(execCommandId).exec(handler);
 * handler.await(archivingTimeout);
 *
 * if (handler.hasError()) {
 *   throw handler.error();
 * }
 * }</pre>
 */
@API(status = Status.INTERNAL)
public final class SyncDockerExecHandler implements ResultCallback<Frame> {
  private final CountDownLatch barrier = new CountDownLatch(1);
  private final Logger logger;

  private Throwable error;
  private Closeable closeable;

  /** @param logger the logger to use when logging the STDOUT or STDERR of the command */
  public SyncDockerExecHandler(final Logger logger) {
    this.logger = logger;
  }

  @Override
  public void onStart(final Closeable closeable) {
    this.closeable = closeable;
  }

  @Override
  public void onNext(final Frame object) {
    switch (object.getStreamType()) {
      case STDIN:
        throw new UnsupportedOperationException();
      case STDERR:
        logger.error("{}", object);
        break;
      case STDOUT:
      case RAW:
      default:
        logger.info("{}", object);
        break;
    }
  }

  @Override
  public void onError(final Throwable throwable) {
    error = throwable;
    barrier.countDown();
  }

  @Override
  public void onComplete() {
    barrier.countDown();
  }

  @Override
  public void close() throws IOException {
    if (closeable != null) {
      closeable.close();
    }

    barrier.countDown();
  }

  /** @return the captured error (if any) */
  public Throwable error() {
    return error;
  }

  /** @return true if there was an error during the command execution, false otherwise */
  public boolean hasError() {
    return error != null;
  }

  /**
   * Waits until the command finished executing on the container, up to a maximum of {@code
   * timeout}, or if the current thread is interrupted.
   *
   * @param timeout the maximum time to wait
   * @throws TimeoutException if the command did not finish within the given {@code duration}
   * @throws InterruptedException if the thread was interrupted while waiting for the command to
   *     finish
   */
  @SuppressWarnings("JavaDoc")
  public void await(final Duration timeout) {
    try {
      if (!barrier.await(timeout.toNanos(), TimeUnit.NANOSECONDS)) {
        error = new TimeoutException(String.format("Timed out after %s", timeout));
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      error = e;
    }
  }
}

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import org.slf4j.event.Level;
import org.testcontainers.containers.Container;
import org.testcontainers.utility.MountableFile;

public interface ZeebeContainer<SELF extends ZeebeContainer<SELF>> extends Container<SELF> {

  default SELF withEnv(final Environment envVar, final String value) {
    return withEnv(envVar.variable(), value);
  }

  default SELF withEnv(final Environment envVar, final boolean value) {
    return withEnv(envVar, String.valueOf(value));
  }

  default SELF withEnv(final Environment envVar, final int value) {
    return withEnv(envVar, String.valueOf(value));
  }

  default SELF withEnv(final Environment envVar, final Collection<String> value) {
    return withEnv(envVar, String.join(",", value));
  }

  default SELF withLogLevel(final Level logLevel) {
    return withEnv(ZeebeEnvironment.ZEEBE_LOG_LEVEL, logLevel.toString());
  }

  default SELF withAtomixLogLevel(final Level logLevel) {
    return withEnv(ZeebeEnvironment.ATOMIX_LOG_LEVEL, logLevel.toString());
  }

  default SELF withCopyFileToContainer(final MountableFile file) {
    return withCopyFileToContainer(file, ZeebeDefaults.getInstance().getDefaultConfigurationPath());
  }

  default SELF withConfigurationResource(final String configurationResource) {
    return withCopyFileToContainer(MountableFile.forClasspathResource(configurationResource));
  }

  default SELF withConfigurationFile(final File configurationFile) {
    return withCopyFileToContainer(MountableFile.forHostPath(configurationFile.getAbsolutePath()));
  }

  default SELF withConfiguration(final InputStream configuration) {
    try {
      final Path tempFile = Files.createTempFile(getClass().getPackage().getName(), ".tmp");
      long bytesRead;
      long offset = 0;
      try (ReadableByteChannel input = Channels.newChannel(configuration);
          FileChannel output = FileChannel.open(tempFile)) {
        while ((bytesRead = output.transferFrom(input, offset, 4096L)) > 0) {
          offset += bytesRead;
        }
      }

      return withConfigurationFile(tempFile.toFile());
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  default SELF withAdvertisedHost(final String advertisedHost) {
    return withEnv(ZeebeEnvironment.ZEEBE_ADVERTISED_HOST, advertisedHost);
  }

  /**
   * Attempts to stop the container gracefully. If it times out, the container is abruptly killed.
   *
   * @param timeout must be greater than 1 second
   */
  default void shutdownGracefully(Duration timeout) {
    getDockerClient()
        .stopContainerCmd(getContainerId())
        .withTimeout((int) timeout.getSeconds())
        .exec();
  }
}

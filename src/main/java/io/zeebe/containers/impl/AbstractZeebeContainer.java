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
package io.zeebe.containers.impl;

import io.zeebe.containers.api.ZeebeContainer;
import io.zeebe.containers.api.ZeebeEnvironment;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Collection;
import org.slf4j.event.Level;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

public abstract class AbstractZeebeContainer<SELF extends AbstractZeebeContainer<SELF>>
    extends GenericContainer<SELF> implements ZeebeContainer<SELF> {

  protected String internalHost;

  public AbstractZeebeContainer(String dockerImageName) {
    super(dockerImageName);
  }

  protected abstract ZeebeEnvironment getZeebeEnvironment();

  protected abstract String getConfigFilePath();

  @Override
  public SELF withLogLevel(final Level logLevel) {
    return withEnv(getZeebeEnvironment().getZeebeLogLevel(), logLevel.toString());
  }

  @Override
  public SELF withAtomixLogLevel(final Level logLevel) {
    return withEnv(getZeebeEnvironment().getAtomixLogLevel(), logLevel.toString());
  }

  @Override
  public SELF withAdvertisedHost(final String advertisedHost) {
    return withEnv(getZeebeEnvironment().getAdvertisedHost(), advertisedHost);
  }

  public SELF withConfigurationResource(final String configurationResource) {
    return withCopyFileToContainer(
        MountableFile.forClasspathResource(configurationResource), getConfigFilePath());
  }

  public SELF withConfigurationFile(final File configurationFile) {
    return withCopyFileToContainer(
        MountableFile.forHostPath(configurationFile.getAbsolutePath()), getConfigFilePath());
  }

  public SELF withConfiguration(final InputStream configuration) {
    try {
      final Path tempFile = Files.createTempFile(getClass().getPackage().getName(), ".tmp");
      long bytesRead;
      long offset = 0;
      try (ReadableByteChannel input = Channels.newChannel(configuration);
          FileChannel output = FileChannel.open(tempFile, StandardOpenOption.WRITE)) {
        while ((bytesRead = output.transferFrom(input, offset, 4096L)) > 0) {
          offset += bytesRead;
        }
      }

      return withConfigurationFile(tempFile.toFile());
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public SELF withEnv(final EnvVar envVar, final String value) {
    return withEnv(envVar.variable(), value);
  }

  public SELF withEnv(final EnvVar envVar, final boolean value) {
    return withEnv(envVar, String.valueOf(value));
  }

  public SELF withEnv(final EnvVar envVar, final int value) {
    return withEnv(envVar, String.valueOf(value));
  }

  public SELF withEnv(final EnvVar envVar, final Duration value) {
    return withEnv(envVar, value.toString());
  }

  public SELF withEnv(final EnvVar envVar, final Collection<String> value) {
    return withEnv(envVar, String.join(",", value));
  }
}

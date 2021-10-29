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
package io.zeebe.containers.archive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecStartCmd;
import com.github.dockerjava.api.model.Frame;
import io.zeebe.containers.util.TinyContainer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.ArgumentCaptor;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.SocatContainer;
import org.testcontainers.utility.MountableFile;

@Execution(ExecutionMode.CONCURRENT)
final class ContainerArchiveBuilderTest {
  @Test
  void shouldFailIfContainerNotStarted() {
    // given
    final SocatContainer container = new SocatContainer();
    final ContainerArchiveBuilder builder = ContainerArchive.builder();

    // when - then
    assertThatCode(() -> builder.withContainer(container))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldUseProvidedClient() {
    // given
    final DockerClient client = spy(DockerClientFactory.instance().client());
    try (final TinyContainer container = new TinyContainer()) {
      container.start();

      // when
      final ContainerArchive archive =
          ContainerArchive.builder()
              .withContainer(container)
              .withClient(client)
              .withContainerPath("/tmp")
              .withArchivePath("/archive.tar.gz")
              .build();

      // then
      verify(client, times(1)).execCreateCmd(container.getContainerId());
      verify(client, times(1)).execStartCmd(anyString());
      assertThat(archive).extracting("client").isSameAs(client);
    }
  }

  @Test
  void shouldCreateArchiveOnBuild(@TempDir final Path tempDir) throws IOException {
    final Path file = tempDir.resolve("file");
    final Path destination = tempDir.resolve("dest");
    final ContainerArchive archive;
    Files.write(file, "hello".getBytes(StandardCharsets.UTF_8));

    try (final TinyContainer container =
        new TinyContainer()
            .withCopyFileToContainer(MountableFile.forHostPath(file), "/tmp/archive/file")) {
      container.start();

      // when
      archive =
          ContainerArchive.builder()
              .withContainer(container)
              .withContainerPath("/tmp/archive")
              .withArchivePath("/archive.tar.gz")
              .build();
      archive.extract(destination);
    }

    // then
    assertThat(destination.resolve("tmp/archive/file")).hasSameBinaryContentAs(file);
  }

  @SuppressWarnings("unchecked")
  @Test
  void shouldTimeoutIfArchivingTakesTooLong() {
    // given
    final DockerClient client = spy(DockerClientFactory.instance().client());
    final ExecStartCmd command = mock(ExecStartCmd.class);
    final ArgumentCaptor<ResultCallback<Frame>> callbackCaptor =
        ArgumentCaptor.forClass(ResultCallback.class);
    when(client.execStartCmd(anyString())).thenReturn(command);
    when(command.exec(callbackCaptor.capture())).thenReturn(null);

    try (final TinyContainer container = new TinyContainer()) {
      container.start();

      // when - build the archive, but never call any methods which would complete the callback
      final ContainerArchiveBuilder builder =
          ContainerArchive.builder()
              .withContainer(container)
              .withClient(client)
              .withContainerPath("/tmp")
              .withArchivePath("/archive.tar.gz")
              .withArchivingTimeout(Duration.ofMillis(500));

      // then
      assertThatCode(builder::build).isInstanceOf(TimeoutException.class);
    }
  }
}

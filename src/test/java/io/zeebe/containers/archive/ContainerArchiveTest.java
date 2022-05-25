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

import io.zeebe.containers.util.TarCreator;
import io.zeebe.containers.util.TinyContainer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.utility.MountableFile;

final class ContainerArchiveTest {

  @Test
  void shouldExtractArchive(@TempDir final Path tempDir) throws IOException {
    final Path file = tempDir.resolve("foo");
    final Path localArchivePath = tempDir.resolve("archive.tar.gz");
    final String remoteArchivePath = "/tmp/archive.tar.gz";
    final Path destination = tempDir.resolve("extracted");
    final ContainerArchive archive;
    Files.write(file, "foo".getBytes(StandardCharsets.UTF_8));
    TarCreator.INSTANCE.createGzipArchive(file, localArchivePath);

    try (final TinyContainer container =
        new TinyContainer()
            .withCopyFileToContainer(
                MountableFile.forHostPath(localArchivePath), remoteArchivePath)) {
      container.start();

      // when
      archive = new ContainerArchive(remoteArchivePath, container);
      archive.extract(destination);
    }

    // then
    assertThat(Paths.get(destination.toString(), file.toString())).hasSameBinaryContentAs(file);
  }

  @Test
  void shouldTransferArchive(@TempDir final Path tempDir) throws IOException {
    final Path file = tempDir.resolve("foo");
    final Path localArchivePath = tempDir.resolve("archive.tar.gz");
    final String remoteArchivePath = "/tmp/archive.tar.gz";
    final Path copiedArchivePath = tempDir.resolve("copied.tar.gz");
    final Path extractedPath = tempDir.resolve("copied");
    final ContainerArchive archive;
    Files.write(file, "foo".getBytes(StandardCharsets.UTF_8));
    TarCreator.INSTANCE.createGzipArchive(file, localArchivePath);

    try (final TinyContainer container =
        new TinyContainer()
            .withCopyFileToContainer(
                MountableFile.forHostPath(localArchivePath), remoteArchivePath)) {
      container.start();

      // when
      archive = new ContainerArchive(remoteArchivePath, container);
      archive.transferTo(copiedArchivePath);
    }

    // then - extract the archive and compare its contents
    try (final InputStream rawInput = Files.newInputStream(copiedArchivePath);
        final GzipCompressorInputStream gzipInput = new GzipCompressorInputStream(rawInput);
        final TarArchiveInputStream tarInput = new TarArchiveInputStream(gzipInput)) {
      TarExtractor.INSTANCE.extract(tarInput, extractedPath);
    }

    assertThat(Paths.get(extractedPath.toString(), file.toString())).hasSameBinaryContentAs(file);
  }
}

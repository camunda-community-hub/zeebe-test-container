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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
final class TarExtractorTest {
  private final TarExtractor extractor = new TarExtractor();

  @Test
  void shouldExtractFile(@TempDir final Path tempDir) throws IOException {
    // given
    final Path file = tempDir.resolve("foo");
    final Path archivePath = tempDir.resolve("test.tar");
    final Path destination = tempDir.resolve("extracted");
    Files.write(file, "foo".getBytes(StandardCharsets.UTF_8));
    TarCreator.INSTANCE.createArchive(file, archivePath);

    // when
    try (final InputStream inputStream = Files.newInputStream(archivePath);
        final TarArchiveInputStream archiveInput = new TarArchiveInputStream(inputStream)) {
      extractor.extract(archiveInput, destination);
    }

    // then
    final Path expectedPath = Paths.get(destination.toString(), file.toString());
    assertThat(expectedPath).hasSameBinaryContentAs(file);
  }

  @Test
  void shouldExtractDirectory(@TempDir final Path tempDir) throws IOException {
    // given
    final Path directory = tempDir.resolve("dir");
    final List<Path> files = Arrays.asList(directory.resolve("foo"), directory.resolve("bar"));
    final Path archivePath = tempDir.resolve("test.tar");
    final Path destination = tempDir.resolve("extracted");
    Files.createDirectories(directory);
    Files.write(files.get(0), "foo".getBytes(StandardCharsets.UTF_8));
    Files.write(files.get(1), "bar".getBytes(StandardCharsets.UTF_8));
    TarCreator.INSTANCE.createArchive(directory, archivePath);

    // when
    try (final InputStream inputStream = Files.newInputStream(archivePath);
        final TarArchiveInputStream archiveInput = new TarArchiveInputStream(inputStream)) {
      extractor.extract(archiveInput, destination);
    }

    // then
    assertThat(Paths.get(destination.toString(), files.get(0).toString()))
        .hasSameBinaryContentAs(files.get(0));
    assertThat(Paths.get(destination.toString(), files.get(1).toString()))
        .hasSameBinaryContentAs(files.get(1));
  }

  @Test
  void shouldExtractNestedDirectory(@TempDir final Path tempDir) throws IOException {
    // given
    final Path directory = tempDir.resolve("dir");
    final List<Path> files = Arrays.asList(directory.resolve("foo"), directory.resolve("bar"));
    final Path nestedDirectory = directory.resolve("nested");
    final Path archivePath = tempDir.resolve("test.tar");
    final Path destination = tempDir.resolve("extracted");
    Files.createDirectories(directory);
    Files.createDirectories(nestedDirectory);
    Files.write(files.get(0), "foo".getBytes(StandardCharsets.UTF_8));
    Files.write(files.get(1), "bar".getBytes(StandardCharsets.UTF_8));
    Files.copy(files.get(0), nestedDirectory.resolve("foo"));
    Files.copy(files.get(1), nestedDirectory.resolve("bar"));
    TarCreator.INSTANCE.createArchive(directory, archivePath);

    // when
    try (final InputStream inputStream = Files.newInputStream(archivePath);
        final TarArchiveInputStream archiveInput = new TarArchiveInputStream(inputStream)) {
      extractor.extract(archiveInput, destination);
    }

    // then
    assertThat(Paths.get(destination.toString(), directory.toString(), "foo"))
        .hasSameBinaryContentAs(files.get(0));
    assertThat(Paths.get(destination.toString(), directory.toString(), "bar"))
        .hasSameBinaryContentAs(files.get(1));
    assertThat(Paths.get(destination.toString(), nestedDirectory.toString(), "foo"))
        .hasSameBinaryContentAs(files.get(0));
    assertThat(Paths.get(destination.toString(), nestedDirectory.toString(), "bar"))
        .hasSameBinaryContentAs(files.get(1));
  }
}

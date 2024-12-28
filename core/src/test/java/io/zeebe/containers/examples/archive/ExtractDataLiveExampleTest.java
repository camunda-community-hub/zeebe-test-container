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
package io.zeebe.containers.examples.archive;

import static org.assertj.core.api.Assertions.assertThat;

import io.zeebe.containers.ZeebeBrokerContainer;
import io.zeebe.containers.archive.ContainerArchive;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
final class ExtractDataLiveExampleTest {
  @AutoClose private static final Network NETWORK = Network.newNetwork();

  @Container
  private final ZeebeBrokerContainer container = new ZeebeBrokerContainer().withNetwork(NETWORK);

  @Test
  void shouldExtractData(@TempDir final Path tempDir) {
    // given
    final ContainerArchive archive = ContainerArchive.builder().withContainer(container).build();

    // when
    archive.extract(tempDir);

    // then
    assertThat(tempDir).isNotEmptyDirectory();
    assertThat(tempDir.resolve("usr/local/zeebe/data")).isNotEmptyDirectory();
  }

  @Test
  void shouldTransferData(@TempDir final Path tempDir) throws IOException {
    // given
    final ContainerArchive archive = ContainerArchive.builder().withContainer(container).build();
    final Path archivePath = tempDir.resolve("archive.tar.gz");

    // when
    archive.transferTo(archivePath);

    // then - verify the archive is a zipped TAR file with its first entry being the default Zeebe
    // data path
    try (final InputStream inputStream = Files.newInputStream(archivePath);
        final GzipCompressorInputStream gzipInput = new GzipCompressorInputStream(inputStream);
        final TarArchiveInputStream tarInput = new TarArchiveInputStream(gzipInput)) {
      final TarArchiveEntry entry = tarInput.getNextEntry();
      assertThat(entry.getName()).isEqualTo("usr/local/zeebe/data/");
      assertThat(entry.isDirectory()).isTrue();
    }
  }
}

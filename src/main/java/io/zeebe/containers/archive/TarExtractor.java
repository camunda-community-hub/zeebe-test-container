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

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/** A utility class which can take an input stream and extract */
@API(status = Status.INTERNAL)
public final class TarExtractor {
  public static final TarExtractor INSTANCE = new TarExtractor();

  /**
   * Extracts the contents of the input stream to the given destination.
   *
   * @param archiveInput the input stream of the archive
   * @param destination the path at which the contents of the archive should be extracted
   * @throws IOException if the archive cannot be read, or the destination is not writable
   */
  public void extract(final TarArchiveInputStream archiveInput, final Path destination)
      throws IOException {
    for (TarArchiveEntry entry = archiveInput.getNextTarEntry();
        entry != null;
        entry = archiveInput.getNextTarEntry()) {
      extractEntry(archiveInput, entry, destination);
    }
  }

  private void extractEntry(
      final TarArchiveInputStream archiveInput, final TarArchiveEntry entry, final Path destination)
      throws IOException {
    if (!archiveInput.canReadEntryData(entry)) {
      throw new IOException(
          String.format(
              "Expected to extract %s from TAR archive, but data cannot be read; possibly the "
                  + "archive is corrupted",
              entry));
    }

    final Path entryPath = destination.resolve(entry.getName());

    if (entry.isDirectory()) {
      Files.createDirectories(entryPath);
      for (final TarArchiveEntry childEntry : entry.getDirectoryEntries()) {
        extractEntry(archiveInput, childEntry, entryPath);
      }

      return;
    }

    Files.createDirectories(entryPath.getParent());
    writeEntry(archiveInput, entry, entryPath);
  }

  @SuppressWarnings("java:S2095") // ensure we don't close the input channel
  private void writeEntry(
      final TarArchiveInputStream archiveInput, final TarArchiveEntry entry, final Path destination)
      throws IOException {
    final ReadableByteChannel input = Channels.newChannel(archiveInput);
    try (final FileChannel output =
        FileChannel.open(destination, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)) {
      output.transferFrom(input, 0, entry.getRealSize());
      output.force(true);
    }
  }
}

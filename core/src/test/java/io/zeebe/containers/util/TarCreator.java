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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.agrona.IoUtil;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

/** A test utility class to create TAR files (optionally zipped). */
public final class TarCreator {
  public static final TarCreator INSTANCE = new TarCreator();
  public static final int EOF = -1;

  /**
   * Creates a new zipped TAR file from the given {@code path}, saved at {@code destination}. The
   * path can be a directory or file; if it's a directory, it will be recursively archived.
   *
   * @param path the path to archive
   * @param destination the path to save the archive at
   * @throws IOException if the archive cannot be written, or the path cannot be read
   */
  public void createGzipArchive(final Path path, final Path destination) throws IOException {
    try (final OutputStream outputStream = Files.newOutputStream(destination);
        final GzipCompressorOutputStream gzipOutput = new GzipCompressorOutputStream(outputStream);
        final TarArchiveOutputStream archive = new TarArchiveOutputStream(gzipOutput)) {
      archivePath(archive, path);
    }
  }

  /**
   * Creates a new TAR file from the given {@code path}, saved at {@code destination}. The path can
   * be a directory or file; if it's a directory, it will be recursively archived.
   *
   * @param path the path to archive
   * @param destination the path to save the archive at
   * @throws IOException if the archive cannot be written, or the path cannot be read
   */
  public void createArchive(final Path path, final Path destination) throws IOException {
    try (final OutputStream outputStream = Files.newOutputStream(destination);
        final TarArchiveOutputStream archive = new TarArchiveOutputStream(outputStream)) {
      archivePath(archive, path);
    }
  }

  private void archivePath(final TarArchiveOutputStream archive, final Path path)
      throws IOException {
    final TarArchiveEntry entry = archive.createArchiveEntry(path.toFile(), path.toString());
    archive.putArchiveEntry(entry);

    if (Files.isDirectory(path)) {
      try (final DirectoryStream<Path> paths = Files.newDirectoryStream(path)) {
        for (final Path subPath : paths) {
          archivePath(archive, subPath);
        }
      }
    } else {
      try (final InputStream input = Files.newInputStream(path)) {
        copyArchive(input, archive);
      }
      archive.closeArchiveEntry();
    }
  }

  private void copyArchive(final InputStream inputStream, final OutputStream archive)
      throws IOException {
    final byte[] buffer = new byte[IoUtil.BLOCK_SIZE];
    int bytesRead;
    while ((bytesRead = inputStream.read(buffer)) != EOF) {
      archive.write(buffer, 0, bytesRead);
    }
  }
}

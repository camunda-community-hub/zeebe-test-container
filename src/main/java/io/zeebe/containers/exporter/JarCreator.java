/*
 * Copyright © 2022 camunda services GmbH (info@camunda.com)
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
package io.zeebe.containers.exporter;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * Creates a JAR based on the given class, added all declared inner types along to ensure they are
 * resolve-able at runtime. This does not add referenced, external types.
 */
@API(status = Status.INTERNAL)
final class JarCreator {
  Path createJar(final Class<?>... requiredClasses) throws IOException {
    final Path destination = Files.createTempFile("exporter", ".jar");
    return createJar(destination, requiredClasses);
  }

  private Path createJar(final Path destination, final Class<?>... requiredClasses)
      throws IOException {
    final Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

    try (final FileChannel channel =
            FileChannel.open(destination, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        final JarOutputStream output =
            new JarOutputStream(Channels.newOutputStream(channel), manifest)) {
      for (final Class<?> requiredClass : requiredClasses) {
        writeClass(output, requiredClass);
      }
    }

    return destination;
  }

  private void writeClass(final JarOutputStream output, final Class<?> requiredClass)
      throws IOException {
    final String filename = requiredClass.getName().replace(".", "/") + ".class";
    output.putNextEntry(new JarEntry(filename));
    output.write(readBytecode(requiredClass));
    output.closeEntry();

    // it's important to write the inner classes as well, as these are serialized in separate files
    // only adding the required class is not enough in that case
    for (final Class<?> declaredClass : requiredClass.getDeclaredClasses()) {
      writeClass(output, declaredClass);
    }
  }

  private byte[] readBytecode(final Class<?> requiredClass) throws IOException {
    final ClassReader reader = new ClassReader(requiredClass.getName());
    final ClassWriter writer = new ClassWriter(reader, 0);
    reader.accept(writer, 0);

    return writer.toByteArray();
  }
}

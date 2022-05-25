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
import java.io.UncheckedIOException;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.testcontainers.utility.MountableFile;

/**
 * A thread-safe singleton instance that will dynamically create an exporter JAR which can be
 * injected into a container, e.g. using {@code
 * GenericContainer#withCopyToContainer(DebugExporterJar.get(), "/tmp/debug-exporter.jar")}.
 *
 * <p>The JAR will contain the {@link DebugExporter} and all relevant classes.
 */
@API(status = Status.INTERNAL)
public final class DebugExporterJar {
  @SuppressWarnings("java:S3077") // we just need a volatile for reading
  private volatile MountableFile exporterJar;

  /**
   * Returns a thread-safe, singleton instance of the exporter JAR.
   *
   * @return mountable exporter JAR
   */
  public static MountableFile get() {
    return SingletonHolder.INSTANCE.getJar();
  }

  private MountableFile getJar() {
    if (exporterJar == null) {
      loadJar();
    }

    return exporterJar;
  }

  private synchronized void loadJar() {
    if (exporterJar != null) {
      return;
    }

    final JarCreator creator = new JarCreator();
    try {
      exporterJar = MountableFile.forHostPath(creator.createJar(DebugExporter.class));
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Use a nested class to hold the singleton instance to ensure thread-safety when the class is
   * loaded.
   */
  private static final class SingletonHolder {
    private static final DebugExporterJar INSTANCE = new DebugExporterJar();
  }
}

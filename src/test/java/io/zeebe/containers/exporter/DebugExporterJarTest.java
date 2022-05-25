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

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.exporter.api.Exporter;
import io.camunda.zeebe.protocol.record.Record;
import io.zeebe.containers.exporter.TestExporterApi.TestContext;
import io.zeebe.containers.exporter.TestExporterApi.TestController;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.MountableFile;

/**
 * Smoke test as a counter-part to integration tests to ensure we can actually load the exporter
 * from an isolated JAR and run that. We cannot reuse Zeebe's JAR class loader, since we want to
 * slightly modify the behavior here and never fallback to the existing classes if they are {@link
 * DebugExporter} or its inner classes.
 */
final class DebugExporterJarTest {
  @SuppressWarnings("unchecked")
  @Test
  void shouldCreateExporterFromJar() throws Exception {
    // given
    final TestContext context = new TestContext();
    final TestController controller = new TestController();
    final MountableFile exporterJar = DebugExporterJar.get();
    final List<Record<?>> records = new ArrayList<>();
    final Record<?> record = new ProtocolFactory().generateRecord();

    // when
    try (final DebugReceiver receiver = new DebugReceiver(records::add, 0)) {
      final ClassLoader classLoader = new TestClassLoader(exporterJar);
      final Class<? extends Exporter> exporterClass =
          (Class<? extends Exporter>) classLoader.loadClass(DebugExporter.class.getName());
      final Exporter exporter = exporterClass.getDeclaredConstructor().newInstance();
      receiver.start();
      context.getConfiguration().getArguments().put("url", receiver.recordsEndpoint().toString());

      exporter.configure(context);
      exporter.open(controller);
      exporter.export(record);
    }

    // then
    assertThat(records).containsExactly(record);
  }

  /**
   * A special class loader which will use the system class loader for classes not found in the JAR,
   * except for the original {@link DebugExporter} and its inner classes.
   */
  private static final class TestClassLoader extends URLClassLoader {

    public TestClassLoader(final MountableFile exporterJar) throws MalformedURLException {
      super(
          new URL[] {
            new URL(
                String.format(
                    "jar:%s!/", Paths.get(exporterJar.getFilesystemPath()).toUri().toURL()))
          });
    }

    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
      synchronized (getClassLoadingLock(name)) {
        Class<?> clazz = findLoadedClass(name);
        if (clazz == null) {
          try {
            clazz = findClass(name);
          } catch (final ClassNotFoundException ex) {
            if (!name.startsWith(DebugExporter.class.getName())) {
              clazz = super.loadClass(name);
            } else {
              throw ex;
            }
          }
        }

        return clazz;
      }
    }
  }
}

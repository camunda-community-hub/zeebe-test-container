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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class JarCreatorTest {
  private final JarCreator creator = new JarCreator();

  @Test
  void shouldCreateJarWithSingleClass() throws IOException, ClassNotFoundException {
    // given

    // when
    final Path jarFile = creator.createJar(TestClassA.class);

    // then
    final ClassLoader testLoader = createJarClassLoader(jarFile);
    final Class<?> testClass = testLoader.loadClass(TestClassA.class.getName());
    assertThat(testClass)
        .as("ensure we did not load the existing class")
        .isNotEqualTo(TestClassA.class)
        .as("should have similar structure as existing class")
        .hasDeclaredFields("fieldA")
        .hasDeclaredMethods("methodA")
        .isFinal();
  }

  @Test
  void shouldCreateJarWithMultipleClasses() throws IOException, ClassNotFoundException {
    // given

    // when
    final Path jarFile = creator.createJar(TestClassA.class, TestClassB.class);

    // then
    final ClassLoader testLoader = createJarClassLoader(jarFile);
    final Class<?> testClassA = testLoader.loadClass(TestClassA.class.getName());
    final Class<?> testClassB = testLoader.loadClass(TestClassA.class.getName());
    assertThat(testClassA)
        .as("ensure we did not load the existing class")
        .isNotEqualTo(TestClassA.class);
    assertThat(testClassB)
        .as("ensure we did not load the existing class")
        .isNotEqualTo(TestClassB.class);
  }

  @Test
  void shouldCreateJarWithInnerClasses() throws IOException, ClassNotFoundException {
    // given

    // when
    final Path jarFile = creator.createJar(Container.class);

    // then
    final ClassLoader testLoader = createJarClassLoader(jarFile);
    final Class<?> container = testLoader.loadClass(Container.class.getName());
    final Class<?> inner = testLoader.loadClass(Container.Inner.class.getName());
    assertThat(container)
        .as("ensure we did not load the existing class")
        .isNotEqualTo(Container.class);
    assertThat(inner)
        .as("ensure we did not load the existing class")
        .isNotEqualTo(Container.Inner.class);
  }

  private ClassLoader createJarClassLoader(final Path jarPath) throws MalformedURLException {
    final URL jarUrl;

    final String expandedPath = jarPath.toUri().toURL().toString();
    jarUrl = new URL(String.format("jar:%s!/", expandedPath));

    // ensure it has no parent, so we don't accidentally find the TestClass defined here
    return new URLClassLoader(new URL[] {jarUrl}, null);
  }

  @SuppressWarnings("unused")
  private static final class TestClassA {
    private final boolean fieldA = false;

    private void methodA() {}
  }

  @SuppressWarnings("unused")
  private static final class TestClassB {
    private final boolean fieldB = false;

    private void methodB() {}
  }

  private static final class Container {
    private final Container.Inner innerField = new Inner();

    private static final class Inner {}
  }
}

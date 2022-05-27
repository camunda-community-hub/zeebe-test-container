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

import io.zeebe.containers.ZeebeDefaults;
import java.io.IOException;
import java.util.Objects;
import org.agrona.LangUtil;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.testcontainers.containers.GenericContainer;

/**
 * A builder for a {@link ContainerArchive} which will also take care of producing the archive from
 * a given path.
 *
 * <p>By default, it's tailored to extract Zeebe data, and so will use {@link
 * ZeebeDefaults#getDefaultDataPath()} as the default container path.
 *
 * <p>Example usage:
 *
 * <pre>@{code
 *   // configure and start your container
 *   final ZeebeBrokerContainer container = new ZeebeBrokerContainer();
 *   container.start();
 *   // generate some actual data...
 *   // extract it to a given destination
 *   final Path destination = Paths.of("/tmp/extractedData");
 *   final ContainerArchive archive = ContainerArchive.builder().withContainer(container).build();
 *   archive.extract(destination);
 * }</pre>
 */
@API(status = Status.EXPERIMENTAL)
public final class ContainerArchiveBuilder {
  @SuppressWarnings("java:S1075") // this is a default value, hard-coding it is fine
  private static final String DEFAULT_ARCHIVE_PATH = "/tmp/data.tar.gz";

  private String containerPath = ZeebeDefaults.getInstance().getDefaultDataPath();
  private String archivePath = DEFAULT_ARCHIVE_PATH;
  private GenericContainer<?> container;

  /**
   * Sets the container on which the archive will be created/referenced. Note that the container
   * must exist, as this method will extract its ID.
   *
   * @param container the container on which the archive will exist
   * @param <T> the type of the container
   * @return this builder for chaining
   * @throws IllegalArgumentException if the container was not created yet
   */
  public <T extends GenericContainer<T>> ContainerArchiveBuilder withContainer(final T container) {
    this.container = Objects.requireNonNull(container);
    return this;
  }

  /**
   * On {@link #build()}, an archive will be generated from the given {@link #containerPath} and
   * will be written to the given {@link #archivePath}.
   *
   * @param archivePath the path at which the archive will be written
   * @return this builder for chaining
   */
  public ContainerArchiveBuilder withArchivePath(final String archivePath) {
    this.archivePath = Objects.requireNonNull(archivePath);
    return this;
  }

  /**
   * Sets the path on the container that should be archived on {@link #build()}.
   *
   * @param containerPath the path that should be archived
   * @return this builder for chaining
   */
  public ContainerArchiveBuilder withContainerPath(final String containerPath) {
    this.containerPath = Objects.requireNonNull(containerPath);
    return this;
  }

  /**
   * Creates an archive at {@link #archivePath} which will contain {@link #containerPath} on the
   * given container.
   *
   * @return a {@link ContainerArchive} instance referencing the archive at {@link #archivePath} on
   *     the given container
   * @throws IllegalArgumentException if no container or container ID was configured
   */
  public ContainerArchive build() {
    if (container == null) {
      throw new IllegalArgumentException(
          "Expected to reference an archive from a container, but no container given");
    }

    if (!container.isCreated()) {
      throw new IllegalArgumentException(
          "Expected to extract data from the given container, but it doesn't exist yet");
    }

    archiveContainerPath();
    return new ContainerArchive(archivePath, container);
  }

  private void archiveContainerPath() {
    try {
      container.execInContainer("tar", "-chzf", archivePath, containerPath);
    } catch (IOException e) {
      LangUtil.rethrowUnchecked(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LangUtil.rethrowUnchecked(e);
    }
  }
}

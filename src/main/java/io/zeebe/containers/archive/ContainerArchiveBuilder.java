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

import com.github.dockerjava.api.DockerClient;
import io.zeebe.containers.ZeebeDefaults;
import io.zeebe.containers.util.SyncDockerExecHandler;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.agrona.LangUtil;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(ContainerArchiveBuilder.class);

  private String containerPath = ZeebeDefaults.getInstance().getDefaultDataPath();
  private String archivePath = DEFAULT_ARCHIVE_PATH;
  private DockerClient client = DockerClientFactory.lazyClient();
  private Duration archivingTimeout = Duration.ofMinutes(1);
  private String containerId;

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
    if (!container.isCreated()) {
      throw new IllegalArgumentException(
          "Expected to extract data from the given container, but it doesn't exist yet");
    }

    return withContainerId(container.getContainerId());
  }

  /**
   * Sets the container on which the archive will be created/referenced. Note that this method
   * expects the container for the given ID to exist already.
   *
   * @param containerId the ID of the container on which the archive will exist
   * @return this builder for chaining
   */
  public ContainerArchiveBuilder withContainerId(final String containerId) {
    this.containerId = containerId;
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
    this.archivePath = archivePath;
    return this;
  }

  /**
   * Sets the Docker client that will be used to create and later extract the archive. Can be
   * omitted, and will default to {@link DockerClientFactory#lazyClient()}.
   *
   * @param client the Docker client to use
   * @return this builder for chaining
   */
  public ContainerArchiveBuilder withClient(final DockerClient client) {
    this.client = client;
    return this;
  }

  /**
   * Sets the path on the container that should be archived on {@link #build()}.
   *
   * @param containerPath the path that should be archived
   * @return this builder for chaining
   */
  public ContainerArchiveBuilder withContainerPath(final String containerPath) {
    this.containerPath = containerPath;
    return this;
  }

  /**
   * Sets the time to wait for the creation of the archive in {@link #build()}. By default, it will
   * wait 1 minute.
   *
   * @param timeout the new timeout to set when archiving
   * @return this builder for chaining
   */
  public ContainerArchiveBuilder withArchivingTimeout(final Duration timeout) {
    this.archivingTimeout = timeout;
    return this;
  }

  /**
   * Creates an archive at {@link #archivePath} which will contain {@link #containerPath} on the
   * container with ID {@link #containerId}.
   *
   * @return a {@link ContainerArchive} instance referencing the archive at {@link #archivePath} on
   *     the container with ID {@link #containerId}
   * @throws IllegalArgumentException if no container or container ID was configured
   */
  public ContainerArchive build() {
    if (containerId == null) {
      throw new IllegalArgumentException(
          "Expected to reference an archive from a container, but no container ID given");
    }

    archiveContainerPath();
    return new ContainerArchive(archivePath, containerId, client);
  }

  private void archiveContainerPath() {
    final String execCommandId =
        client
            .execCreateCmd(containerId)
            .withCmd("tar", "-chzf", archivePath, containerPath)
            .withAttachStdout(true)
            .withAttachStderr(true)
            .exec()
            .getId();
    final SyncDockerExecHandler execHandler = new SyncDockerExecHandler(LOGGER);
    client.execStartCmd(execCommandId).exec(execHandler);
    execHandler.await(archivingTimeout);

    if (execHandler.hasError()) {
      final Throwable error = execHandler.error();
      if (error instanceof TimeoutException) {
        final TimeoutException descriptiveError =
            new TimeoutException(
                String.format(
                    "Timed out archiving '%s' on '%s' after '%s'; if there are no outstanding "
                        + "errors in the logs, consider increasing the archiving timeout "
                        + "via #withArchivingTimeout(Duration)",
                    containerPath, containerId, archivingTimeout));
        LangUtil.rethrowUnchecked(descriptiveError);
      }

      LangUtil.rethrowUnchecked(error);
    }
  }
}

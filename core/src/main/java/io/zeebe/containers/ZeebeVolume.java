/*
 * Copyright © 2019 camunda services GmbH (info@camunda.com)
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
package io.zeebe.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateVolumeCmd;
import com.github.dockerjava.api.command.CreateVolumeResponse;
import com.github.dockerjava.api.command.RemoveVolumeCmd;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.SELContext;
import com.github.dockerjava.api.model.Volume;
import io.zeebe.containers.archive.ContainerArchive;
import io.zeebe.containers.archive.ContainerArchiveBuilder;
import io.zeebe.containers.util.TinyContainer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.ResourceReaper;

/**
 * A simple wrapper to create Docker volumes which are managed by Testcontainers. The created object
 * labels the volumes with {@link DockerClientFactory#DEFAULT_LABELS} so that the Ryuk container can
 * reap the volumes should our JVM process crash.
 *
 * <p>NOTE: there is currently an issue with the close method - there may be a race condition
 * between closing the containers where the volume is mounted and closing/removing the volume
 * itself, causing the close method to throw an error.
 */
@API(status = Status.EXPERIMENTAL)
public class ZeebeVolume implements AutoCloseable, ZeebeData {

  private final String name;
  private final DockerClient client;

  /**
   * @see ZeebeVolume#newVolume()
   * @param name the name of the volume
   * @param client the docker client to use
   */
  protected ZeebeVolume(final String name, final DockerClient client) {
    this.name = name;
    this.client = client;
  }

  /** Returns the name of the volume */
  public String getName() {
    return name;
  }

  /**
   * Returns the volume as a bind which can be used when creating new containers.
   *
   * @param mountPath the path where to mount the volume in the container
   * @return a bind which can be used when creating a container
   */
  public Bind asBind(final String mountPath) {
    return new Bind(name, new Volume(mountPath), AccessMode.rw, SELContext.none);
  }

  /**
   * Convenience method which mounts the volume to a Zeebe broker's data folder.
   *
   * @param command the create command of the Zeebe broker container
   */
  public void attachVolumeToContainer(final CreateContainerCmd command) {
    attachVolumeToContainer(command, ZeebeDefaults.getInstance().getDefaultDataPath());
  }

  /**
   * Convenience method which mounts the volume to a Zeebe broker's data folder.
   *
   * @param command the create command of the Zeebe broker container
   */
  public void attachVolumeToContainer(final CreateContainerCmd command, final String mountPath) {
    final HostConfig hostConfig = Objects.requireNonNull(command.getHostConfig());
    final Bind[] binds = hostConfig.getBinds();
    final Bind[] newBinds = new Bind[binds.length + 1];

    System.arraycopy(binds, 0, newBinds, 0, binds.length);
    newBinds[binds.length] = asBind(mountPath);

    command.withHostConfig(hostConfig.withBinds(newBinds));
  }

  /**
   * Removes the volume from Docker.
   *
   * @throws com.github.dockerjava.api.exception.NotFoundException if no such volume exists
   * @throws com.github.dockerjava.api.exception.ConflictException if the volume is currently in use
   */
  @Override
  public void close() {
    try (final RemoveVolumeCmd command = client.removeVolumeCmd(name)) {
      command.exec();
    }
  }

  /**
   * Convenience method to extract the data from this volume, whether it is or isn't already
   * attached to a container. This will start a tiny container which will only serves to extract the
   * data of this volume.
   *
   * <p>If it's already attached to a container, consider using the {@link
   * ContainerArchive#builder()} directly.
   *
   * @param destination the destination to extract the contents of this volume to
   */
  public void extract(final Path destination) {
    extract(destination, UnaryOperator.identity());
  }

  /**
   * Convenience method to extract the data from this volume, whether it is or isn't already
   * attached to a container. This will start a tiny container which will only serves to extract the
   * data of this volume.
   *
   * <p>If it's already attached to a container, consider using the {@link
   * ContainerArchive#builder()} directly.
   *
   * @param destination the destination to extract the contents of this volume to
   * @param modifier an operator which takes in a pre-configured builder and can modify it
   */
  public void extract(
      final Path destination, final UnaryOperator<ContainerArchiveBuilder> modifier) {
    try (final TinyContainer container = new TinyContainer()) {
      container.withCreateContainerCmdModifier(this::attachVolumeToContainer);
      container.start();

      final ContainerArchiveBuilder builder = ContainerArchive.builder().withContainer(container);
      final ContainerArchive archive = modifier.apply(builder).build();

      archive.extract(destination);
    }
  }

  /** Returns a new default managed volume */
  public static ZeebeVolume newVolume() {
    return newVolume(UnaryOperator.identity());
  }

  /**
   * @param configurator a function which can optionally configure more of the volume
   * @return a new managed volume using the given Docker client to create it
   */
  public static ZeebeVolume newVolume(final UnaryOperator<CreateVolumeCmd> configurator) {
    final DockerClient client = DockerClientFactory.instance().client();
    final Map<String, String> labels = new HashMap<>();
    labels.putAll(DockerClientFactory.DEFAULT_LABELS);
    //noinspection deprecation
    labels.putAll(ResourceReaper.instance().getLabels());

    try (final CreateVolumeCmd command = client.createVolumeCmd().withLabels(labels)) {
      final CreateVolumeResponse response = configurator.apply(command).exec();
      return new ZeebeVolume(response.getName(), client);
    }
  }

  @Override
  public <T extends GenericContainer<T> & ZeebeBrokerNode<T>> void attach(final T container) {
    container.withCreateContainerCmdModifier(this::attachVolumeToContainer);
  }
}

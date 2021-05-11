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

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectVolumeResponse;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.SocatContainer;

final class ZeebeVolumeTest {
  @Test
  void shouldCreateVolumeWithTestcontainersLabels() {
    // given
    final Map<String, String> expectedLabels = DockerClientFactory.DEFAULT_LABELS;
    final DockerClient client = DockerClientFactory.lazyClient();

    // when
    final ZeebeVolume volume = ZeebeVolume.newVolume();
    final InspectVolumeResponse response = client.inspectVolumeCmd(volume.getName()).exec();

    // then
    assertThat(response.getLabels())
        .as("the volume should contain the base Testcontainers labels to be reaped on shutdown")
        .containsExactlyInAnyOrderEntriesOf(expectedLabels);
  }

  @Test
  void shouldCreateVolumeWithName() {
    // given
    final String name = "test";
    final DockerClient client = DockerClientFactory.lazyClient();

    // when
    final ZeebeVolume volume = ZeebeVolume.newVolume(vol -> vol.withName(name));
    final InspectVolumeResponse response = client.inspectVolumeCmd(volume.getName()).exec();

    // then
    assertThat(response)
        .as("the volume should exist and have the right name")
        .isNotNull()
        .extracting(InspectVolumeResponse::getName)
        .isEqualTo(name);
  }

  @Test
  void shouldAttachVolumeToContainerWithDefaultBindPath() {
    // given
    // use the SocatContainer as a lightweight container which does not exit immediately so we can
    // inspect its volumes
    final DockerClient client = DockerClientFactory.lazyClient();
    try (final GenericContainer<?> container = new SocatContainer().withTarget(8080, "fakeHost")) {
      final ZeebeVolume volume = ZeebeVolume.newVolume();

      // when
      container.withCreateContainerCmdModifier(volume::attachVolumeToContainer);
      container.start();

      // then
      assertVolumeIsCorrectlyMounted(client, volume, container.getContainerId());
    }
  }

  @Test
  void shouldAttachToZeebeBroker() {
    // given
    final DockerClient client = DockerClientFactory.lazyClient();
    try (final ZeebeBrokerContainer container = new ZeebeBrokerContainer()) {
      final ZeebeVolume volume = ZeebeVolume.newVolume();

      // when
      container.withZeebeData(volume);
      container.start();

      // then
      assertVolumeIsCorrectlyMounted(client, volume, container.getContainerId());
    }
  }

  private void assertVolumeIsCorrectlyMounted(
      final DockerClient client, final ZeebeVolume volume, final String containerId) {
    final InspectContainerResponse containerResponse =
        client.inspectContainerCmd(containerId).exec();
    final Bind[] volumeBinds = containerResponse.getHostConfig().getBinds();
    assertThat(volumeBinds).hasSize(1);

    final Bind bind = volumeBinds[0];
    assertThat(bind.getAccessMode())
        .as("the volume should be mounted in read-write")
        .isEqualTo(AccessMode.rw);
    assertThat(bind.getPath())
        .as("the bind's path should be the volume name")
        .isEqualTo(volume.getName());
    assertThat(bind.getVolume().getPath())
        .as("the volume path should be the default Zeebe data path")
        .isEqualTo(ZeebeDefaults.getInstance().getDefaultDataPath());
  }
}

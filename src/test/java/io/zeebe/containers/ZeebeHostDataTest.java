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
import com.github.dockerjava.api.command.InspectContainerResponse.Mount;
import com.github.dockerjava.api.model.Volume;
import io.zeebe.containers.util.TestUtils;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.DockerClientFactory;

final class ZeebeHostDataTest {
  @Test
  void shouldAttachToZeebeContainer(final @TempDir Path dataDir) {
    // given
    final DockerClient client = DockerClientFactory.lazyClient();
    final String runAsUser = TestUtils.getRunAsUser();

    // when
    final InspectContainerResponse response;
    try (final ZeebeBrokerContainer container = new ZeebeBrokerContainer()) {
      final ZeebeHostData data = new ZeebeHostData(dataDir.toString());
      // configure the broker to use the same UID and GID as our current user so we can remove the
      // temporary directory at the end
      container
          .withZeebeData(data)
          .self()
          .withCreateContainerCmdModifier(cmd -> cmd.withUser(runAsUser));
      container.start();

      response = client.inspectContainerCmd(container.getContainerId()).exec();
    }

    // then
    assertThat(response.getMounts())
        .as("there should be exactly one bind mount, the host data")
        .hasSize(1);

    final Mount mount = Objects.requireNonNull(response.getMounts()).get(0);
    assertThat(mount.getRW()).as("the host data should be mounted as read-write").isTrue();
    assertThat(mount.getSource())
        .as("the mount's source should be the host data directory")
        .isEqualTo(dataDir.toString());
    assertThat(mount.getDestination())
        .as("the mount's destination should be the default data path in the Zeebe container")
        .isNotNull()
        .extracting(Volume::getPath)
        .isEqualTo(ZeebeDefaults.getInstance().getDefaultDataPath());
  }
}

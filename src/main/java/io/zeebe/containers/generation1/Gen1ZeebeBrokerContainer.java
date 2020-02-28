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
package io.zeebe.containers.generation1;

import de.skuzzle.semantic.Version;
import io.zeebe.containers.api.ZeebeBrokerEnvironment;
import io.zeebe.containers.impl.AbstractZeebeBrokerContainer;
import java.util.Objects;

@SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
public class Gen1ZeebeBrokerContainer
    extends AbstractZeebeBrokerContainer<Gen1ZeebeBrokerContainer> {

  private static final ZeebeBrokerEnvironment BROKER_ENVIRONMENT = new Gen1ZeebeBrokerEnvironment();

  public Gen1ZeebeBrokerContainer() {
    this(Gen1ZeebeDefaults.DEFAULT_ZEEBE_VERSION);
  }

  public Gen1ZeebeBrokerContainer(final Version version) {
    super(version);
  }

  public Gen1ZeebeBrokerContainer(final String dockerImageNameWithTag) {
    super(dockerImageNameWithTag);
  }

  @Override
  protected ZeebeBrokerEnvironment getZeebeBrokerEnvironment() {
    return BROKER_ENVIRONMENT;
  }

  @Override
  protected String getConfigFilePath() {
    return Gen1ZeebeDefaults.DEFAULT_CONFIGURATION_PATH_BROKER;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    if (!super.equals(o)) {
      return false;
    }

    final Gen1ZeebeBrokerContainer that = (Gen1ZeebeBrokerContainer) o;
    return portOffset == that.portOffset
        && embedGateway == that.embedGateway
        && Objects.equals(host, that.host);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), host, portOffset, embedGateway);
  }
}

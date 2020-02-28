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
import io.zeebe.containers.impl.AbstractZeebeTestContainerFactory;

public class Gen1ZeebeTestContainerFacory
    extends AbstractZeebeTestContainerFactory<
        Gen1ZeebeTestContainerFacory, Gen1ZeebeGatewayContainer, Gen1ZeebeBrokerContainer> {

  public Gen1ZeebeTestContainerFacory(final Version version) {
    super(version);
  }

  @Override
  public Gen1ZeebeGatewayContainer createGatewayContainer(final String dockerImageNameWithTag) {
    return new Gen1ZeebeGatewayContainer(dockerImageNameWithTag);
  }

  @Override
  public Gen1ZeebeBrokerContainer createBrokerContainer(final String dockerImageNameWithTag) {
    return new Gen1ZeebeBrokerContainer(dockerImageNameWithTag);
  }
}

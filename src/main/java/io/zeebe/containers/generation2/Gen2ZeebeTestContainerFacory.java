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
package io.zeebe.containers.generation2;

import de.skuzzle.semantic.Version;
import io.zeebe.containers.impl.AbstractZeebeTestContainerFactory;

public class Gen2ZeebeTestContainerFacory
    extends AbstractZeebeTestContainerFactory<
        Gen2ZeebeTestContainerFacory, Gen2ZeebeGatewayContainer, Gen2ZeebeBrokerContainer> {

  public Gen2ZeebeTestContainerFacory(final Version version) {
    super(version);
  }

  @Override
  public Gen2ZeebeGatewayContainer createGatewayContainer(final String dockerImageNameWithTag) {
    return new Gen2ZeebeGatewayContainer(dockerImageNameWithTag);
  }

  @Override
  public Gen2ZeebeBrokerContainer createBrokerContainer(final String dockerImageNameWithTag) {
    return new Gen2ZeebeBrokerContainer(dockerImageNameWithTag);
  }
}

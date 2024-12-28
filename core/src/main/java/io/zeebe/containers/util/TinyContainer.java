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
package io.zeebe.containers.util;

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A tiny container which simply starts an interactive, long living alpine with basic busybox
 * utilities. Useful as a file copy container, or archiving container, etc.
 */
@API(status = Status.INTERNAL)
public final class TinyContainer extends GenericContainer<TinyContainer> {
  private static final DockerImageName IMAGE = DockerImageName.parse("alpine:3.14.2");

  /** Configures a new container using an Alpine based image */
  public TinyContainer() {
    super(IMAGE);
  }

  @SuppressWarnings("resource")
  @Override
  protected void configure() {
    super.configure();
    withCommand("cat").withCreateContainerCmdModifier(cmd -> cmd.withTty(true));
  }
}

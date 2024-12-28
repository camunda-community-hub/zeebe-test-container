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
package io.zeebe.containers.util;

import com.github.dockerjava.api.model.Info;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.DisabledIf;
import org.testcontainers.DockerClientFactory;

@API(status = Status.INTERNAL)
public final class TestcontainersSupport {
  private TestcontainersSupport() {}

  /**
   * Returns whether tests run against Testcontainers Cloud or not by checking the server version.
   *
   * <p>This isn't perfect as it's clearly breaking implementation details, but it works for now.
   */
  public static boolean isCloudEnv() {
    if (System.getenv().containsKey("TC_CLOUD_TOKEN")) {
      return true;
    }

    //noinspection resource
    final Info dockerInfo = DockerClientFactory.lazyClient().infoCmd().exec();
    return dockerInfo.getServerVersion() != null
        && dockerInfo.getServerVersion().endsWith("testcontainerscloud");
  }

  @DisabledIf(
      value = "io.zeebe.containers.util.TestcontainersSupport#isCloudEnv",
      disabledReason = "Testcontainers Cloud does not support mounting host files")
  @Target({ElementType.TYPE, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @Tag("local-test")
  public @interface DisabledIfTestcontainersCloud {}
}

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
package io.zeebe.containers.engine;

import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.filters.RecordStream;
import java.util.Optional;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.testcontainers.lifecycle.TestDescription;
import org.testcontainers.lifecycle.TestLifecycleAware;

/**
 * Extend {@link ContainerEngine} to be aware of the test lifecycle in order to initialize {@link
 * BpmnAssert} and print out the record stream on error.
 *
 * <p>The Testcontainers extension uses the {@link TestLifecycleAware} marker annotation and will
 * call the methods accordingly.
 */
@API(status = Status.INTERNAL)
interface TestAwareContainerEngine extends ContainerEngine, TestLifecycleAware {
  @Override
  default void beforeTest(final TestDescription description) {
    BpmnAssert.initRecordStream(RecordStream.of(getRecordStreamSource()));
  }

  @Override
  default void afterTest(final TestDescription description, final Optional<Throwable> throwable) {
    if (throwable.isPresent()) {
      RecordStream.of(getRecordStreamSource()).print(true);
    }
  }
}

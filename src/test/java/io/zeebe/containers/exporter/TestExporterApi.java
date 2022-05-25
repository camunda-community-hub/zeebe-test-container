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
package io.zeebe.containers.exporter;

import io.camunda.zeebe.exporter.api.context.Configuration;
import io.camunda.zeebe.exporter.api.context.Context;
import io.camunda.zeebe.exporter.api.context.Controller;
import io.camunda.zeebe.exporter.api.context.ScheduledTask;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: replace this with io.camunda.zeebe:zeebe-exporter-test:8.1.0-alpha2 when available
final class TestExporterApi {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestExporterApi.class);

  private TestExporterApi() {}

  static final class TestContext implements Context {
    private final TestConfig config = new TestConfig();

    @Override
    public Logger getLogger() {
      return LOGGER;
    }

    @Override
    public Configuration getConfiguration() {
      return config;
    }

    @Override
    public void setFilter(final RecordFilter filter) {}
  }

  static final class TestConfig implements Configuration {
    private final Map<String, Object> arguments = new HashMap<>();

    @Override
    public String getId() {
      return "debug";
    }

    @Override
    public Map<String, Object> getArguments() {
      return arguments;
    }

    @Override
    public <T> T instantiate(final Class<T> configClass) {
      throw new UnsupportedOperationException("Not yet implemented");
    }
  }

  static final class TestController implements Controller {
    private volatile long position;

    long position() {
      return position;
    }

    @Override
    public void updateLastExportedRecordPosition(final long position) {
      this.position = position;
    }

    @Override
    public ScheduledTask scheduleCancellableTask(final Duration delay, final Runnable task) {
      throw new UnsupportedOperationException("Not yet implemented");
    }
  }
}

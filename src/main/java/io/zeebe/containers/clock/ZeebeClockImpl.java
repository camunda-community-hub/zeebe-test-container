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
package io.zeebe.containers.clock;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.Feign;
import feign.Retryer;
import feign.Target;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;
import io.zeebe.containers.clock.ZeebeClockClient.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/**
 * The default {@link ZeebeClock} implementation which uses a Feign based client, {@link
 * ZeebeClockClient}, to interact with some {@link io.zeebe.containers.ZeebeNode}'s actor clock
 * actuator.
 */
@API(status = Status.INTERNAL)
final class ZeebeClockImpl implements ZeebeClock {
  private static final Slf4jLogger LOGGER = new Slf4jLogger(ZeebeClockImpl.class);

  private final ZeebeClockClient client;

  ZeebeClockImpl(final Target<ZeebeClockClient> target) {
    final List<Module> jacksonModules = Collections.singletonList(new JavaTimeModule());
    client =
        Feign.builder()
            .encoder(new JacksonEncoder(jacksonModules))
            .decoder(new JacksonDecoder(jacksonModules))
            .logger(LOGGER)
            .retryer(Retryer.NEVER_RETRY)
            .target(target);
  }

  @Override
  public Instant pinTime(final Instant time) {
    final long epochMilli = time.toEpochMilli();
    return performClientCall(c -> c.pinTime(epochMilli));
  }

  @Override
  public Instant addTime(final Duration offset) {
    final long offsetMilli = offset.toMillis();
    return performClientCall(c -> c.addTime(offsetMilli));
  }

  @Override
  public Instant getCurrentTime() {
    return performClientCall(ZeebeClockClient::getCurrentTime);
  }

  @Override
  public Instant resetTime() {
    return performClientCall(ZeebeClockClient::resetTime);
  }

  private Instant performClientCall(final Function<ZeebeClockClient, Response> call) {
    try {
      final Response response = call.apply(client);
      return response.instant;
    } catch (final Exception e) {
      throw new ZeebeClockException(e);
    }
  }
}

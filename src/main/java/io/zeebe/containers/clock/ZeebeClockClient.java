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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonProperty;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import java.time.Instant;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/** Client specification for Zeebe's actor clock actuator. */
@API(status = Status.INTERNAL)
interface ZeebeClockClient {
  @RequestLine("GET /actuator/clock")
  @Headers("Accept: application/json")
  Response getCurrentTime();

  @RequestLine("POST /actuator/clock/pin")
  @Headers({"Content-Type: application/json", "Accept: application/json"})
  Response pinTime(@Param("epochMilli") long epochMilli);

  @RequestLine("POST /actuator/clock/add")
  @Headers({"Content-Type: application/json", "Accept: application/json"})
  Response addTime(@Param("offsetMilli") long offsetMilli);

  @RequestLine("DELETE /actuator/clock")
  @Headers("Accept: application/json")
  Response resetTime();

  final class Response {
    @JsonProperty("epochMilli")
    final long epochMilli;

    @JsonProperty("instant")
    final Instant instant;

    @JsonCreator(mode = Mode.PROPERTIES)
    Response(
        final @JsonProperty("epochMilli") long epochMilli,
        final @JsonProperty("instant") Instant instant) {
      this.epochMilli = epochMilli;
      this.instant = instant;
    }
  }
}

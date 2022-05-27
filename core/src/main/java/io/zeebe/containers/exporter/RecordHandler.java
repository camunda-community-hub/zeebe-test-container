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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.protocol.jackson.ZeebeProtocolModule;
import io.camunda.zeebe.protocol.record.Record;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.Message;
import org.apache.hc.core5.http.message.BasicHttpResponse;
import org.apache.hc.core5.http.nio.AsyncRequestConsumer;
import org.apache.hc.core5.http.nio.AsyncResponseProducer;
import org.apache.hc.core5.http.nio.AsyncServerRequestHandler;
import org.apache.hc.core5.http.nio.entity.BasicAsyncEntityConsumer;
import org.apache.hc.core5.http.nio.entity.BasicAsyncEntityProducer;
import org.apache.hc.core5.http.nio.support.BasicRequestConsumer;
import org.apache.hc.core5.http.nio.support.BasicResponseProducer;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP request handler for the /records endpoint. It accepts uncompressed, application/json
 * content. The content is expected to be a list of serialized {@link Record} objects, all coming
 * from the same partition, which it forwards to the {@link #recordConsumer}.
 *
 * <p>The response can be one of:
 *
 * <ul>
 *   <li>200 - the records were passed through, and the response body will be a singleton map with
 *       one key, <em>position</em>, the value of which is the highest acknowledged position for the
 *       partition ID from which the records are coming from
 *   <li>204 - there were either no records passed, or there is no known acknowledged position yet
 *       for the partition form which the records are coming from
 *   <li>400 - if there is no request body, or the request body cannot be parsed as a list of
 *       records
 * </ul>
 */
@API(status = Status.INTERNAL)
final class RecordHandler implements AsyncServerRequestHandler<Message<HttpRequest, byte[]>> {
  private static final Logger LOGGER = LoggerFactory.getLogger(RecordHandler.class);
  private static final ObjectMapper MAPPER =
      new ObjectMapper().registerModule(new ZeebeProtocolModule());

  private final Consumer<Record<?>> recordConsumer;
  private final Map<Integer, Long> positions = new HashMap<>();

  RecordHandler(final Consumer<Record<?>> recordConsumer) {
    this.recordConsumer = Objects.requireNonNull(recordConsumer, "must specify a record consumer");
  }

  void acknowledge(final int partitionId, final long position) {
    positions.put(partitionId, position);
  }

  @Override
  public AsyncRequestConsumer<Message<HttpRequest, byte[]>> prepare(
      final HttpRequest request, final EntityDetails entityDetails, final HttpContext context) {
    return new BasicRequestConsumer<>(new BasicAsyncEntityConsumer());
  }

  @Override
  public void handle(
      final Message<HttpRequest, byte[]> requestObject,
      final ResponseTrigger responseTrigger,
      final HttpContext context)
      throws HttpException, IOException {
    final byte[] requestBody = requestObject.getBody();
    if (requestBody == null || requestBody.length == 0) {
      final BasicHttpResponse response =
          new BasicHttpResponse(HttpStatus.SC_BAD_REQUEST, "must send a list of records as body");
      responseTrigger.submitResponse(new BasicResponseProducer(response), context);
      return;
    }

    final List<Record<?>> records;
    try {
      records = MAPPER.readValue(requestBody, new TypeReference<List<Record<?>>>() {});
    } catch (final IOException e) {
      final BasicHttpResponse response =
          new BasicHttpResponse(
              HttpStatus.SC_BAD_REQUEST,
              "failed to deserialize records, see receiver logs for more");
      responseTrigger.submitResponse(new BasicResponseProducer(response), context);
      LOGGER.warn("Failed to deserialize exported records", e);

      return;
    }

    if (records.isEmpty()) {
      final BasicHttpResponse response =
          new BasicHttpResponse(HttpStatus.SC_NO_CONTENT, "no records given");
      responseTrigger.submitResponse(new BasicResponseProducer(response), context);
      return;
    }

    records.forEach(recordConsumer);

    final int partitionId = records.get(0).getPartitionId();
    final AsyncResponseProducer responseProducer = createSuccessfulResponse(partitionId);
    responseTrigger.submitResponse(responseProducer, context);
  }

  private AsyncResponseProducer createSuccessfulResponse(final int partitionId)
      throws JsonProcessingException {
    final Long position = positions.get(partitionId);

    if (position == null) {
      final HttpResponse response =
          new BasicHttpResponse(
              HttpStatus.SC_NO_CONTENT, "no acknowledged position for partition " + partitionId);
      return new BasicResponseProducer(response);
    }

    final HttpResponse response = new BasicHttpResponse(HttpStatus.SC_OK);
    response.setHeader("Content-Type", "application/json");
    final byte[] responseBody =
        MAPPER.writeValueAsBytes(Collections.singletonMap("position", position));
    return new BasicResponseProducer(response, new BasicAsyncEntityProducer(responseBody));
  }
}

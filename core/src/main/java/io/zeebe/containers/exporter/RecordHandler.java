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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.protocol.jackson.ZeebeProtocolModule;
import io.camunda.zeebe.protocol.record.Record;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
 *   <li>200 - there were either no records passed, or there is no known acknowledged position yet
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
  private static final byte[] EMPTY_BODY = "{}".getBytes(StandardCharsets.UTF_8);

  private final Consumer<Record<?>> recordConsumer;
  private final boolean autoAcknowledge;
  private final Map<Integer, Long> positions = new ConcurrentHashMap<>();

  RecordHandler(final Consumer<Record<?>> recordConsumer, final boolean autoAcknowledge) {
    this.recordConsumer = Objects.requireNonNull(recordConsumer, "must specify a record consumer");
    this.autoAcknowledge = autoAcknowledge;
  }

  void acknowledge(final int partitionId, final long position) {
    positions.merge(partitionId, position, Math::max);
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
    final AsyncResponseProducer responseProducer = handleRequest(requestBody);
    responseTrigger.submitResponse(responseProducer, context);
  }

  private AsyncResponseProducer handleRequest(final byte[] requestBody)
      throws JsonProcessingException {
    if (requestBody == null || requestBody.length == 0) {
      return createErrorResponse(HttpStatus.SC_BAD_REQUEST, "Must send a list of records as body");
    }

    final List<Record<?>> records;
    try {
      records = MAPPER.readValue(requestBody, new TypeReference<List<Record<?>>>() {});
    } catch (final IOException e) {
      LOGGER.warn("Failed to deserialize exported records", e);
      return createErrorResponse(
          HttpStatus.SC_BAD_REQUEST, "Failed to deserialize records, see receiver logs for more");
    }

    if (records.isEmpty()) {
      LOGGER.debug("No records given, will return a successful response regardless");
    }

    for (final Record<?> record : records) {
      recordConsumer.accept(record);

      if (autoAcknowledge) {
        acknowledge(record.getPartitionId(), record.getPosition());
      }
    }

    return createSuccessfulResponse(records);
  }

  private AsyncResponseProducer createSuccessfulResponse(final List<Record<?>> records)
      throws JsonProcessingException {
    final HttpResponse response = new BasicHttpResponse(HttpStatus.SC_OK);
    final byte[] responseBody =
        records.isEmpty()
            ? EMPTY_BODY
            : MAPPER.writeValueAsBytes(
                Collections.singletonMap(
                    "position", positions.get(records.get(0).getPartitionId())));

    response.setHeader("Content-Type", "application/json; charset=UTF-8");
    return new BasicResponseProducer(response, new BasicAsyncEntityProducer(responseBody));
  }

  private AsyncResponseProducer createErrorResponse(final int status, final String message)
      throws JsonProcessingException {
    final ProblemDetail problem = new ProblemDetail(status, message);
    final HttpResponse response = new BasicHttpResponse(status);
    response.setHeader("Content-Type", "application/json; charset=UTF-8");

    final byte[] responseBody = MAPPER.writeValueAsBytes(problem);
    return new BasicResponseProducer(response, new BasicAsyncEntityProducer(responseBody));
  }

  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  private static final class ProblemDetail {
    private final String type = "about:blank";
    private final String instance = "/records";

    private final int status;
    private final String detail;

    private ProblemDetail(int status, String detail) {
      this.status = status;
      this.detail = detail;
    }
  }
}

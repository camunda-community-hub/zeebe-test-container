package io.zeebe.containers.test;

import feign.Headers;
import feign.RequestLine;
import io.camunda.zeebe.protocol.jackson.record.AbstractRecord;
import java.util.List;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/** Client specification for Zeebe's debug HTTP exporter */
@API(status = Status.INTERNAL)
interface DebugExporterClient {
  @RequestLine("GET /records.json")
  @Headers({"Content-Type: application/json", "Accept: application/json"})
  List<AbstractRecord<?>> getRecords();
}

package io.zeebe.containers.test;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.Feign;
import feign.FeignException.FeignClientException;
import feign.Request.Options;
import feign.Retryer;
import feign.Target;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;
import io.camunda.zeebe.protocol.jackson.record.AbstractRecord;
import io.camunda.zeebe.protocol.record.Record;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.lifecycle.Startable;

final class DebugExporterConsumer implements AutoCloseable, Startable {
  private static final Logger LOGGER = LoggerFactory.getLogger(DebugExporterConsumer.class);

  private final Collection<DebugExporterClient> clients;
  private final ConcurrentNavigableMap<RecordId, Record<?>> records;
  private final Lock lock;
  private final Condition isEmptyCondition;

  private ScheduledExecutorService executorService;

  DebugExporterConsumer(
      final Collection<Target<DebugExporterClient>> targets,
      final Lock lock,
      final Condition isEmptyCondition) {
    clients = targets.stream().map(this::createClient).collect(Collectors.toList());
    this.records = new ConcurrentSkipListMap<>();
    this.lock = lock;
    this.isEmptyCondition = isEmptyCondition;
  }

  @Override
  public void start() {
    executorService = newExecutor();
    executorService.submit(this::consumeRecords);
  }

  @Override
  public void stop() {
    executorService.shutdownNow();
  }

  @SuppressWarnings("java:S1452")
  ConcurrentNavigableMap<RecordId, Record<?>> getRecords() {
    return records;
  }

  private void consumeRecords() {
    final int currentRecordsCount = records.size();

    // will block until all streams are collected
    //noinspection ResultOfMethodCallIgnored
    clients.parallelStream()
        .map(this::getRecords)
        .flatMap(List::stream)
        .collect(() -> records, (map, r) -> map.put(new RecordId(r), r), Map::putAll);

    // notify threads waiting for new records that there are, in fact, new records
    if (records.size() > currentRecordsCount) {
      try {
        lock.lockInterruptibly();
        isEmptyCondition.signalAll();
      } catch (final InterruptedException ignored) { // NOSONAR
        Thread.currentThread().interrupt();
      } finally {
        lock.unlock();
      }
    }
    executorService.schedule(this::consumeRecords, 500, TimeUnit.MILLISECONDS);
  }

  private DebugExporterClient createClient(final Target<DebugExporterClient> target) {
    final List<Module> jacksonModules = Collections.singletonList(new JavaTimeModule());
    final Options requestOptions = new Options(1, TimeUnit.SECONDS, 5, TimeUnit.SECONDS, true);

    return Feign.builder()
        .encoder(new JacksonEncoder(jacksonModules))
        .decoder(new JacksonDecoder(jacksonModules))
        .logger(new Slf4jLogger(DebugExporterConsumer.class))
        .retryer(Retryer.NEVER_RETRY)
        .options(requestOptions)
        .target(target);
  }

  private ScheduledExecutorService newExecutor() {
    final RejectedExecutionHandler silentlyDiscardPolicy = new DiscardPolicy();
    final ThreadFactory threadFactory = r -> new Thread(r, "debug-exporter-consumer");

    return new ScheduledThreadPoolExecutor(1, threadFactory, silentlyDiscardPolicy);
  }

  private List<AbstractRecord<?>> getRecords(final DebugExporterClient client) {
    try {
      return client.getRecords();
    } catch (final FeignClientException e) {
      LOGGER.trace("Failed to get records from {}", client, e);
      return Collections.emptyList();
    }
  }
}

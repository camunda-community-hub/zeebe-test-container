package io.zeebe.containers.test;

import io.camunda.zeebe.process.test.api.RecordStreamSource;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordValue;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.value.DeploymentRecordValue;
import io.camunda.zeebe.protocol.record.value.IncidentRecordValue;
import io.camunda.zeebe.protocol.record.value.JobBatchRecordValue;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import io.camunda.zeebe.protocol.record.value.MessageRecordValue;
import io.camunda.zeebe.protocol.record.value.MessageStartEventSubscriptionRecordValue;
import io.camunda.zeebe.protocol.record.value.MessageSubscriptionRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessMessageSubscriptionRecordValue;
import io.camunda.zeebe.protocol.record.value.TimerRecordValue;
import io.camunda.zeebe.protocol.record.value.VariableDocumentRecordValue;
import io.camunda.zeebe.protocol.record.value.VariableRecordValue;
import io.camunda.zeebe.protocol.record.value.deployment.Process;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ConsumerRecordStreamSource implements RecordStreamSource, Iterable<Record<?>> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerRecordStreamSource.class);
  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

  private final DebugExporterConsumer consumer;
  private final Lock lock;
  private final Condition isEmptyCondition;

  private Duration timeout;

  ConsumerRecordStreamSource(
      final DebugExporterConsumer consumer, final Lock lock, final Condition isEmptyCondition) {
    this(consumer, lock, isEmptyCondition, DEFAULT_TIMEOUT);
  }

  ConsumerRecordStreamSource(
      final DebugExporterConsumer consumer,
      final Lock lock,
      final Condition isEmptyCondition,
      final Duration timeout) {
    this.consumer = consumer;
    this.timeout = timeout;
    this.lock = lock;
    this.isEmptyCondition = isEmptyCondition;
  }

  void setTimeout(final Duration timeout) {
    this.timeout = timeout;
  }

  @Override
  public Iterator<Record<?>> iterator() {
    return new DelayedIterator(consumer.getRecords(), lock, isEmptyCondition, timeout);
  }

  @Override
  public Iterable<Record<?>> records() {
    return this;
  }

  @Override
  public Iterable<Record<ProcessInstanceRecordValue>> processInstanceRecords() {
    return recordsOfValueType(ValueType.PROCESS_INSTANCE);
  }

  @Override
  public Iterable<Record<JobRecordValue>> jobRecords() {
    return recordsOfValueType(ValueType.JOB);
  }

  @Override
  public Iterable<Record<JobBatchRecordValue>> jobBatchRecords() {
    return recordsOfValueType(ValueType.JOB_BATCH);
  }

  @Override
  public Iterable<Record<DeploymentRecordValue>> deploymentRecords() {
    return recordsOfValueType(ValueType.DEPLOYMENT);
  }

  @Override
  public Iterable<Record<Process>> processRecords() {
    return recordsOfValueType(ValueType.PROCESS);
  }

  @Override
  public Iterable<Record<VariableRecordValue>> variableRecords() {
    return recordsOfValueType(ValueType.VARIABLE);
  }

  @Override
  public Iterable<Record<VariableDocumentRecordValue>> variableDocumentRecords() {
    return recordsOfValueType(ValueType.VARIABLE_DOCUMENT);
  }

  @Override
  public Iterable<Record<IncidentRecordValue>> incidentRecords() {
    return recordsOfValueType(ValueType.INCIDENT);
  }

  @Override
  public Iterable<Record<TimerRecordValue>> timerRecords() {
    return recordsOfValueType(ValueType.TIMER);
  }

  @Override
  public Iterable<Record<MessageRecordValue>> messageRecords() {
    return recordsOfValueType(ValueType.MESSAGE);
  }

  @Override
  public Iterable<Record<MessageSubscriptionRecordValue>> messageSubscriptionRecords() {
    return recordsOfValueType(ValueType.MESSAGE_SUBSCRIPTION);
  }

  @Override
  public Iterable<Record<MessageStartEventSubscriptionRecordValue>>
      messageStartEventSubscriptionRecords() {
    return recordsOfValueType(ValueType.MESSAGE_START_EVENT_SUBSCRIPTION);
  }

  @Override
  public Iterable<Record<ProcessMessageSubscriptionRecordValue>>
      processMessageSubscriptionRecords() {
    return recordsOfValueType(ValueType.PROCESS_MESSAGE_SUBSCRIPTION);
  }

  @Override
  public void print(final boolean compact) {
    final Collection<Record<?>> records = Collections.unmodifiableCollection(consumer.getRecords().values());

    // not sure that I want to add a dependency to test-util
    LOGGER.debug("===== records (count: {}) =====", records.size());
    records.forEach(r -> LOGGER.debug(r.toJson()));
    LOGGER.debug("---------------------------");
  }

  @SuppressWarnings("unchecked")
  <T extends RecordValue> Iterable<Record<T>> recordsOfValueType(final ValueType valueType) {
    final Stream<Record<T>> stream =
        StreamSupport.stream(records().spliterator(), false)
            .filter(r -> r.getValueType() == valueType)
            .map(r -> (Record<T>) r);
    return stream::iterator;
  }
}

package io.zeebe.containers.test;

import io.camunda.zeebe.protocol.record.Record;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * An iterator which wraps another iterator, allowing for a small blocking delay when calling {@link
 * #hasNext()} if there is not yet a next element. If an element is produced while it's waiting,
 * then {@link #hasNext()} will return true. Use this to stream from asynchronous sources for
 * example.
 */
final class DelayedIterator implements Iterator<Record<?>> {

  private final ConcurrentNavigableMap<RecordId, Record<?>> records;
  private final Lock lock;
  private final Condition isEmptyCondition;
  private final long timeoutMs;

  private RecordId currentKey = new RecordId(0, 0, 0);
  private RecordId nextKey;

  DelayedIterator(
      final ConcurrentNavigableMap<RecordId, Record<?>> records,
      final Lock lock,
      final Condition isEmptyCondition,
      final Duration timeout) {
    this.records = records;
    this.lock = lock;
    this.isEmptyCondition = isEmptyCondition;

    timeoutMs = timeout.toMillis();
  }

  @Override
  public boolean hasNext() {
    try {
      lock.lockInterruptibly();

      final Date deadline = Date.from(Instant.now().plusMillis(timeoutMs));
      boolean stillWaiting = true;

      nextKey = records.higherKey(currentKey);
      while (nextKey == null && stillWaiting) {
        stillWaiting = isEmptyCondition.awaitUntil(deadline);
        nextKey = records.higherKey(currentKey);
      }

      return nextKey != null;
    } catch (final InterruptedException ignored) { // NOSONAR
      Thread.currentThread().interrupt();
    } finally {
      lock.unlock();
    }

    return false;
  }

  @Override
  public Record<?> next() {
    if (nextKey == null) {
      throw new NoSuchElementException();
    }

    currentKey = nextKey;
    nextKey = null;
    return records.get(currentKey);
  }
}

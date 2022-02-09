package io.zeebe.containers.test;

import io.camunda.zeebe.protocol.record.Record;
import java.util.Comparator;
import java.util.Objects;

final class RecordId implements Comparable<RecordId> {

  private final long timestamp;
  private final int partitionId;
  private final long position;

  RecordId(final Record<?> zeebeRecord) {
    this(zeebeRecord.getTimestamp(), zeebeRecord.getPartitionId(), zeebeRecord.getPosition());
  }

  RecordId(final long timestamp, final int partitionId, final long position) {
    this.timestamp = timestamp;
    this.partitionId = partitionId;
    this.position = position;
  }

  @Override
  public int compareTo(final RecordId other) {
    return Comparator.<RecordId>comparingLong(r -> r.timestamp)
        .thenComparingInt(r -> r.partitionId)
        .thenComparingLong(r -> r.position)
        .compare(this, other);
  }

  @Override
  public int hashCode() {
    return Objects.hash(partitionId, position);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof RecordId)) {
      return false;
    }

    final RecordId recordId = (RecordId) o;
    return partitionId == recordId.partitionId && position == recordId.position;
  }

  @Override
  public String toString() {
    return "RecordId{"
        + "timestamp="
        + timestamp
        + ", partitionId="
        + partitionId
        + ", position="
        + position
        + '}';
  }
}

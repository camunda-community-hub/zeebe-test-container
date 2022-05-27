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

import io.camunda.zeebe.protocol.record.ImmutableProtocol;
import io.camunda.zeebe.protocol.record.ImmutableRecord;
import io.camunda.zeebe.protocol.record.ImmutableRecord.Builder;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.RecordValue;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.Intent;
import io.camunda.zeebe.protocol.util.ValueTypeMapping;
import io.camunda.zeebe.protocol.util.ValueTypeMapping.Mapping;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.FieldPredicates;
import org.jeasy.random.api.Randomizer;
import org.jeasy.random.randomizers.AbstractRandomizer;
import org.jeasy.random.randomizers.range.LongRangeRandomizer;
import org.jeasy.random.randomizers.registry.CustomRandomizerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** TODO: replace with actual factory from zeebe-protocol-test-util on 8.1.0 release. */
@SuppressWarnings("java:S1452")
public final class ProtocolFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolFactory.class);
  private static final String PROTOCOL_PACKAGE_NAME = Record.class.getPackage().getName() + "*";

  private final CustomRandomizerRegistry randomizerRegistry;
  private final EasyRandomParameters parameters;
  private final EasyRandom random;

  public ProtocolFactory() {
    randomizerRegistry = new CustomRandomizerRegistry();
    parameters = getDefaultParameters().seed(0L);
    random = new EasyRandom(parameters);
    registerRandomizers();
  }

  public <T extends RecordValue> Stream<Record<T>> generateRecords() {
    return generateRecords(UnaryOperator.identity());
  }

  public <T extends RecordValue> Stream<Record<T>> generateRecords(
      final UnaryOperator<Builder<T>> modifier) {
    return Stream.generate(() -> generateRecord(modifier));
  }

  public <T extends RecordValue> Stream<Record<T>> generateForAllValueTypes() {
    return generateForAllValueTypes(UnaryOperator.identity());
  }

  public <T extends RecordValue> Stream<Record<T>> generateForAllValueTypes(
      final UnaryOperator<Builder<T>> modifier) {
    return ValueTypeMapping.getAcceptedValueTypes().stream()
        .map(valueType -> generateRecord(valueType, modifier));
  }

  public <T extends RecordValue> Record<T> generateRecord() {
    return generateRecord(UnaryOperator.identity());
  }

  public <T extends RecordValue> Record<T> generateRecord(
      final UnaryOperator<Builder<T>> modifier) {
    final ValueType valueType = random.nextObject(ValueType.class);
    return generateRecord(valueType, modifier);
  }

  public <T extends RecordValue> Record<T> generateRecord(final ValueType valueType) {
    return generateRecord(valueType, UnaryOperator.identity());
  }

  public <T extends RecordValue> Record<T> generateRecord(
      final ValueType valueType, final UnaryOperator<Builder<T>> modifier) {
    return generateImmutableRecord(valueType, modifier);
  }

  public <T> T generateObject(final Class<T> objectClass) {
    return random.nextObject(objectClass);
  }

  public long getSeed() {
    return parameters.getSeed();
  }

  private void registerRandomizers() {
    findProtocolTypes().forEach(this::registerProtocolType);
    randomizerRegistry.registerRandomizer(Object.class, new RawObjectRandomizer());

    // restrict longs to be between 0 and max value - this is because many of our long properties
    // are timestamps, which are semantically between 0 and any future time
    randomizerRegistry.registerRandomizer(
        Long.class, new LongRangeRandomizer(0L, Long.MAX_VALUE, getSeed()));
    randomizerRegistry.registerRandomizer(
        long.class, new LongRangeRandomizer(0L, Long.MAX_VALUE, getSeed()));

    // never use NULL_VAL or SBE_UNKNOWN for ValueType or RecordType
    randomizerRegistry.registerRandomizer(
        ValueType.class,
        new EnumRandomizer<>(
            getSeed(), ValueTypeMapping.getAcceptedValueTypes().toArray(new ValueType[0])));

    final EnumSet<RecordType> excludedRecordTypes =
        EnumSet.of(RecordType.NULL_VAL, RecordType.SBE_UNKNOWN);
    final Set<RecordType> recordTypes = EnumSet.complementOf(excludedRecordTypes);
    randomizerRegistry.registerRandomizer(
        RecordType.class, new EnumRandomizer<>(getSeed(), recordTypes.toArray(new RecordType[0])));
  }

  private void registerProtocolType(final ClassInfo abstractType) {
    final List<Class<?>> implementations =
        abstractType
            .getClassesImplementing()
            .filter(info -> info.getSimpleName().startsWith("Immutable"))
            .directOnly()
            .loadClasses();

    if (implementations.isEmpty()) {
      LOGGER.warn(
          "No implementations found for abstract protocol type {}; random generation will not be possible for this type",
          abstractType.getName());
      return;
    }

    final Class<?> implementation = implementations.get(0);
    if (implementations.size() > 1) {
      LOGGER.warn(
          "More than one implementation found for abstract protocol type {}; random generation will use the first one: {}",
          abstractType.getName(),
          implementation.getName());
    }

    randomizerRegistry.registerRandomizer(
        abstractType.loadClass(), () -> random.nextObject(implementation));
  }

  private EasyRandomParameters getDefaultParameters() {
    // as we will ensure value/intent/valueType having matching types, omit them from the
    // randomization process - we will do that individually afterwards
    final Predicate<Field> excludedRecordFields =
        FieldPredicates.inClass(ImmutableRecord.class)
            .and(
                FieldPredicates.named("value")
                    .or(FieldPredicates.named("intent"))
                    .or(FieldPredicates.named("valueType")));

    return new EasyRandomParameters()
        .randomizerRegistry(randomizerRegistry)
        // we have to bypass the setters since our types are almost exclusively immutable
        .bypassSetters(true)
        // allow empty collections, and only generate up to 5 items
        .collectionSizeRange(0, 5)
        // as we have nested types in our protocol, let's give a generous depth here, but let's
        // still limit it to avoid errors/issues with nested collections
        .randomizationDepth(8)
        .excludeField(excludedRecordFields);
  }

  @SuppressWarnings("rawtypes")
  private <T extends RecordValue> Record<T> generateImmutableRecord(
      final ValueType valueType, final UnaryOperator<Builder<T>> modifier) {
    Objects.requireNonNull(valueType, "must specify a value type");
    Objects.requireNonNull(modifier, "must specify a builder modifier");

    final Mapping<?, ?> typeInfo = ValueTypeMapping.get(valueType);
    final Intent intent = random.nextObject(typeInfo.getIntentClass());
    final RecordValue value = generateObject(typeInfo.getValueClass());
    final Record seedRecord = random.nextObject(Record.class);

    //noinspection unchecked
    final Builder<T> builder =
        ImmutableRecord.builder()
            .from(seedRecord)
            .withValueType(valueType)
            .withValue(value)
            .withIntent(intent);

    return Objects.requireNonNull(modifier.apply(builder), "must return a non null builder")
        .build();
  }

  // visible for testing
  static ClassInfoList findProtocolTypes() {
    return new ClassGraph()
        .acceptPackages(PROTOCOL_PACKAGE_NAME)
        .enableAnnotationInfo()
        .scan()
        .getAllInterfaces()
        .filter(info -> info.hasAnnotation(ImmutableProtocol.class.getName()))
        .directOnly();
  }

  private static final class EnumRandomizer<E extends Enum<E>> extends AbstractRandomizer<E> {
    private final E[] values;

    EnumRandomizer(final long seed, final E[] values) {
      super(seed);
      this.values = Objects.requireNonNull(values, "must specify some enum values");
    }

    @Override
    public E getRandomValue() {
      if (values.length == 0) {
        return null;
      }

      final int index = random.nextInt(values.length);
      return values[index];
    }
  }

  private final class RawObjectRandomizer implements Randomizer<Object> {
    private final Class<?>[] variableTypes = new Class[] {Boolean.class, Long.class, String.class};

    @Override
    public Object getRandomValue() {
      final Class<?> variableType = variableTypes[random.nextInt(variableTypes.length)];
      return random.nextObject(variableType);
    }
  }
}

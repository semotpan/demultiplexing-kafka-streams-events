package com.semotpan.kevents;

import com.semotpan.kevents.eventhandler.EventHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.data.RecordBuilder;
import org.apache.avro.specific.SpecificRecord;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public final class EventAggregator<K extends SpecificRecord, D extends SpecificRecord> {

    private final Map<Class<? extends SpecificRecord>, EventHandler> handlerProvider;
    private final Function<D, RecordBuilder<D>> destinationBuilder;
    private final Predicate<D> eventProcessingValidator;

    public EventAggregator(final List<EventHandler> handlers,
                           final Function<D, RecordBuilder<D>> destinationBuilder,
                           final Predicate<D> eventProcessingValidator) {

        this.handlerProvider = Objects.requireNonNull(handlers).stream()
                .collect(Collectors.toMap(EventHandler::eventClass, Function.identity()));

        this.destinationBuilder = Objects.requireNonNull(destinationBuilder);
        this.eventProcessingValidator = Objects.requireNonNull(eventProcessingValidator);
    }

    public boolean filter(final K key, final SpecificRecord event) {
        return null != event && handlerProvider.containsKey(event.getClass()) && handlerProvider.get(event.getClass()).filter(event);
    }

    public K changeKey(final K key, final SpecificRecord event) {
        return (K) handlerProvider.get(event.getClass()).changeKey(key, event);
    }

    public D aggregate(final K key, final SpecificRecord event, final D destination) {
        log.info("Processing event class: '{}' value: {}", event.getClass().getSimpleName(), event);
        final RecordBuilder<D> builder = destinationBuilder.apply(destination);

        try {
            handlerProvider.get(event.getClass()).handle(event, builder);
            return builder.build();
        } catch (final AvroRuntimeException ignored) {

            // validate previous state if fails delete created entity
            if (!eventProcessingValidator.test(destination)) {
                log.error("Error processing event {}", event);
                return null;
            }

            // get previous state
            return destination;
        }
    }
}

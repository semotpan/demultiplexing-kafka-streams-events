package com.semotpan.kevents.eventhandler;

import org.apache.avro.specific.SpecificRecord;

public interface EventHandler<E, D> {

    Class<E> eventClass();

    SpecificRecord changeKey(final SpecificRecord key, final E event);

    void handle(E event, D destination);

    default boolean filter(final E event) {
        return true;
    }

}

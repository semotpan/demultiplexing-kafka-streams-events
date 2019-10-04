package com.semotpan.kevents.eventhandler;

import com.semotpan.avro.UserCreateEvent;
import com.semotpan.avro.UserEntity;
import org.apache.avro.specific.SpecificRecord;

public final class UserCreateEventHandler implements EventHandler<UserCreateEvent, UserEntity.Builder> {

    @Override
    public Class<UserCreateEvent> eventClass() {
        return UserCreateEvent.class;
    }

    @Override
    public SpecificRecord changeKey(final SpecificRecord key, final UserCreateEvent event) {
        return key;
    }

    @Override
    public void handle(final UserCreateEvent event, final UserEntity.Builder destination) {
        destination.setUserId(event.getUserId())
                .setFirstName(event.getFirstName())
                .setLastName(event.getLastName())
                .setEmail(event.getEmail());
    }
}

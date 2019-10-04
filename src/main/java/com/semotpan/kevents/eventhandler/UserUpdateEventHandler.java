package com.semotpan.kevents.eventhandler;

import com.semotpan.avro.UnaryKey;
import com.semotpan.avro.UserEntity;
import com.semotpan.avro.UserUpdateEvent;
import org.apache.avro.specific.SpecificRecord;

public final class UserUpdateEventHandler implements EventHandler<UserUpdateEvent, UserEntity.Builder> {

    @Override
    public Class<UserUpdateEvent> eventClass() {
        return UserUpdateEvent.class;
    }

    @Override
    public UnaryKey changeKey(final SpecificRecord key, final UserUpdateEvent event) {
        return new UnaryKey(((UnaryKey)key).getId());
    }

    @Override
    public void handle(final UserUpdateEvent event, final UserEntity.Builder destination) {
        destination.setFirstName(event.getFirstName())
                .setLastName(event.getLastName());
    }
}

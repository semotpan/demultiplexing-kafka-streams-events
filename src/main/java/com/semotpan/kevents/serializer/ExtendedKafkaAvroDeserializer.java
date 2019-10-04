package com.semotpan.kevents.serializer;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.ExtendedDeserializer;

import java.util.Map;

public final class ExtendedKafkaAvroDeserializer implements ExtendedDeserializer<Object> {

    private static final String PARENT_TOPIC = "PARENT-TOPIC";

    private final Deserializer<Object> deserializer = new KafkaAvroDeserializer();

    @Override
    public Object deserialize(final String topic, final Headers headers, final byte[] data) {
        return deserialize(topic(topic, headers), data);
    }

    private String topic(final String topic, final Headers headers) {
        return null != headers.lastHeader(PARENT_TOPIC) ? new String(headers.lastHeader(PARENT_TOPIC).value()) : topic;
    }


    @Override
    public void configure(final Map<String, ?> configs, final boolean isKey) {
        deserializer.configure(configs, isKey);
    }

    @Override
    public Object deserialize(final String topic, final byte[] data) {
        return deserializer.deserialize(topic, data);
    }

    @Override
    public void close() {
        deserializer.close();
    }
}
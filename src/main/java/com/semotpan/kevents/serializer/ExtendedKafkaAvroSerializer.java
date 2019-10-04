package com.semotpan.kevents.serializer;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.ExtendedSerializer;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

public final class ExtendedKafkaAvroSerializer implements ExtendedSerializer<Object> {

    private static final String PARENT_TOPIC = "PARENT-TOPIC";
    private final Serializer<Object> serializer = new KafkaAvroSerializer();

    @Override
    public byte[] serialize(final String topic, final Headers headers, final Object data) {
        return serialize(topic(topic, headers), data);
    }

    private String topic(final String topic, final Headers headers) {
        return null != headers.lastHeader(PARENT_TOPIC) ? new String(headers.lastHeader(PARENT_TOPIC).value()) : topic;
    }

    @Override
    public void configure(final Map<String, ?> configs, boolean isKey) {
        serializer.configure(configs, isKey);
    }

    @Override
    public byte[] serialize(final String topic, final Object data) {
        return serializer.serialize(topic, data);
    }

    @Override
    public void close() {
        serializer.close();
    }
}

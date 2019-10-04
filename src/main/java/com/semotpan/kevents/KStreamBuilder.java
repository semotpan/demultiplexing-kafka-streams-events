package com.semotpan.kevents;

import com.semotpan.avro.UnaryKey;
import com.semotpan.avro.UserEntity;
import lombok.AllArgsConstructor;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Objects;

@Component
@AllArgsConstructor
public final class KStreamBuilder {

    private final StreamsBuilder streamBuilder;
    private final EventAggregator<UnaryKey, UserEntity> userEventAggregator;

    @PostConstruct
    public void initStream() {

        final KStream<UnaryKey, SpecificRecord> stream = streamBuilder.stream("user");

        stream.filter(userEventAggregator::filter)
                .groupBy(userEventAggregator::changeKey)
                .aggregate(UserEntity::new, userEventAggregator::aggregate)
                .toStream()
                .filter((key, value) -> Objects.nonNull(value))
                .to("userEntity");
    }
}

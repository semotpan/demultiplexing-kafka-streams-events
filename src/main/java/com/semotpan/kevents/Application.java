package com.semotpan.kevents;

import com.semotpan.avro.UnaryKey;
import com.semotpan.avro.UserEntity;
import com.semotpan.kevents.eventhandler.EventHandler;
import com.semotpan.kevents.eventhandler.UserCreateEventHandler;
import com.semotpan.kevents.eventhandler.UserUpdateEventHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean(DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration defaultKafkaStreamsConfig(final KafkaProperties kafkaProperties) {
        return new KafkaStreamsConfiguration(kafkaProperties.buildStreamsProperties());
    }

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_BUILDER_BEAN_NAME)
    public StreamsBuilderFactoryBean defaultKafkaStreamsBuilder(@Qualifier(DEFAULT_STREAMS_CONFIG_BEAN_NAME) final ObjectProvider<KafkaStreamsConfiguration> streamsConfigProvider) {

        if (null != streamsConfigProvider.getIfAvailable()) {
            return new StreamsBuilderFactoryBean(streamsConfigProvider.getIfAvailable());
        }

        throw new UnsatisfiedDependencyException(KafkaStreamsDefaultConfiguration.class.getName(),
                KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_BUILDER_BEAN_NAME, "streamsConfig",
                "There is no '" + DEFAULT_STREAMS_CONFIG_BEAN_NAME + "' bean in the application context.\n");
    }

    @Bean
    public EventAggregator<UnaryKey, UserEntity> userEventAggregator() {
        return new EventAggregator<>(eventHandlerList(), userEntity -> UserEntity.newBuilder(), userEntity -> null != userEntity.getUserId());
    }

    private List<EventHandler> eventHandlerList() {
        final List<EventHandler> eventHandlers = new ArrayList<>(2);
        eventHandlers.add(new UserCreateEventHandler());
        eventHandlers.add(new UserUpdateEventHandler());
        return eventHandlers;
    }
}

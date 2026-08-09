package com.datalog.watchlog.config;

import com.datalog.watchlog.event.LogEventMessage;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka wiring for the log pipeline: topic definition plus JSON-serialized
 * producer/consumer. Configured in code so {@code application.properties} only
 * needs to set {@code spring.kafka.bootstrap-servers}.
 */
@Configuration
public class KafkaConfig {

    /** Topic the ingestion API produces to and the log indexer reads from. */
    public static final String LOG_EVENTS_TOPIC = "log-events";

    /** Consumer group of the log indexer. */
    public static final String LOG_INDEXER_GROUP = "log-indexer";

    private static final String TRUSTED_PACKAGES = "com.datalog.watchlog.event";

    @Value("${spring.kafka.bootstrap-servers:localhost:9000}")
    private String bootstrapServers;

    @Bean
    public NewTopic logEventsTopic() {
        return new NewTopic(LOG_EVENTS_TOPIC, 1, (short) 1);
    }

    @Bean
    public ProducerFactory<String, LogEventMessage> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, LogEventMessage> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, LogEventMessage> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, LOG_INDEXER_GROUP);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, TRUSTED_PACKAGES);
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(LogEventMessage.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, LogEventMessage> kafkaListenerContainerFactory(
            ConsumerFactory<String, LogEventMessage> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, LogEventMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}

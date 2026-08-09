package com.datalog.watchlog.config;

import com.datalog.watchlog.event.LogEventMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
 * producer/consumer.
 *
 * <p>Both serializers use a dedicated {@link ObjectMapper} with
 * {@link JavaTimeModule} registered (and ISO-8601 dates). spring-kafka's
 * {@code JsonSerializer} would otherwise fall back to a bare mapper that cannot
 * handle {@code java.time.Instant} fields like {@code LogEventMessage.timestamp}.
 */
@Configuration
public class KafkaConfig {

    /** Topic the ingestion API produces to and the log indexer reads from. */
    public static final String LOG_EVENTS_TOPIC = "log-events";

    /** Consumer group of the log indexer. */
    public static final String LOG_INDEXER_GROUP = "log-indexer";

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
        JsonSerializer<LogEventMessage> serializer = new JsonSerializer<>(kafkaObjectMapper());
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), serializer);
    }

    @Bean
    public KafkaTemplate<String, LogEventMessage> kafkaTemplate(
            ProducerFactory<String, LogEventMessage> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ConsumerFactory<String, LogEventMessage> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, LOG_INDEXER_GROUP);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        JsonDeserializer<LogEventMessage> deserializer =
                new JsonDeserializer<>(LogEventMessage.class, kafkaObjectMapper(), false);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, LogEventMessage> kafkaListenerContainerFactory(
            ConsumerFactory<String, LogEventMessage> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, LogEventMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    /**
     * Mapper with jsr310 support, independent of Spring Boot's auto-configured one,
     * so Kafka serialization never depends on web-layer configuration.
     */
    private static ObjectMapper kafkaObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}

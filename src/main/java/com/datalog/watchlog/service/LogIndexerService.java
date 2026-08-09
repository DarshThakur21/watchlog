package com.datalog.watchlog.service;

import com.datalog.watchlog.config.KafkaConfig;
import com.datalog.watchlog.document.LogDocument;
import com.datalog.watchlog.event.LogEventMessage;
import com.datalog.watchlog.repository.LogDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Reads log events off the {@code log-events} topic and indexes them into
 * Elasticsearch. This is what decouples ingestion from search: Kafka holds the
 * backlog when ES is slow or down, and the consumer scales out by adding more
 * instances in the same group.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LogIndexerService {

    private final LogDocumentRepository logDocumentRepository;

    @KafkaListener(topics = KafkaConfig.LOG_EVENTS_TOPIC, groupId = KafkaConfig.LOG_INDEXER_GROUP)
    public void index(LogEventMessage event) {
        LogDocument document = event.toDocument();
        logDocumentRepository.save(document);
        log.debug("Indexed log document {} for service {}", document.getId(), document.getServiceId());
    }
}

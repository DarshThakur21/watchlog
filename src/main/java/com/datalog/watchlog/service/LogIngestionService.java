package com.datalog.watchlog.service;

import com.datalog.watchlog.config.KafkaConfig;
import com.datalog.watchlog.dto.LogIngestRequest;
import com.datalog.watchlog.event.LogEventMessage;
import com.datalog.watchlog.model.Services;
import com.datalog.watchlog.repository.ServicesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates an incoming log request, resolves the owning project, and produces a
 * {@link LogEventMessage} onto the {@code log-events} topic. The indexer consumer
 * (Step 6) reads it from there and writes it to Elasticsearch.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LogIngestionService {

    private final ServicesRepository servicesRepository;
    private final KafkaTemplate<String, LogEventMessage> kafkaTemplate;

    public void ingest(LogIngestRequest request) {
        Services service = servicesRepository.findById(request.serviceId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Service not found: " + request.serviceId()));

        LogEventMessage event = new LogEventMessage(
                service.getServiceId().toString(),
                service.getProject().getProjectId().toString(),
                request.timestamp(),
                request.level().name(),
                request.logger(),
                request.thread(),
                request.message());

        kafkaTemplate.send(KafkaConfig.LOG_EVENTS_TOPIC, event.serviceId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish log event for service {}", event.serviceId(), ex);
                    }
                });
    }
}

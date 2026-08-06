package com.datalog.watchlog.event;

import com.datalog.watchlog.document.LogDocument;

import java.time.Instant;

public record LogEventMessage(

    String serviceId,
    String projectId,
    Instant timestamp,
    String level,
    String logger,
    String thread,
    String message
){
    /**
     * Helper method to map this incoming event directly into an Elasticsearch LogDocument
     */
    public LogDocument toDocument() {
        return LogDocument.builder()
                .serviceId(this.serviceId)
                .projectId(this.projectId)
                .timestamp(this.timestamp != null ? this.timestamp : Instant.now())
                .level(this.level)
                .logger(this.logger)
                .thread(this.thread)
                .message(this.message)
                .build();
    }
}



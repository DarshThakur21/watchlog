package com.datalog.watchlog.dto;

import com.datalog.watchlog.model.enums.LogLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;
import java.util.UUID;

/**
 * Filters for the log query API ({@code GET /api/logs}).
 *
 * @param serviceId optional filter on a single service
 * @param level     optional filter on severity
 * @param from      inclusive lower bound on timestamp (defaults to unbounded)
 * @param to        exclusive upper bound on timestamp (defaults to unbounded)
 * @param keyword   optional full-text search on the message
 * @param page      zero-based page number (defaults to 0)
 * @param size      page size (defaults to 20)
 */
public record LogQueryRequest(
        UUID serviceId,
        LogLevel level,
        Instant from,
        Instant to,
        String keyword,
        @Min(0) Integer page,
        @Min(1) @Max(500) Integer size) {

    public LogQueryRequest {
        if (page == null) {
            page = 0;
        }
        if (size == null) {
            size = 20;
        }
    }
}

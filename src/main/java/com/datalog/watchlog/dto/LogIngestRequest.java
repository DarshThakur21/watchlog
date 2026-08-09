package com.datalog.watchlog.dto;

import com.datalog.watchlog.model.enums.LogLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * The HTTP body sent by an appender to {@code POST /api/logs}.
 *
 * Mirrors the shape of the {@code LogEventMessage} Kafka payload (minus
 * {@code projectId}, which is resolved server-side from {@code serviceId}).
 *
 * @param serviceId the service that produced the log line
 * @param timestamp when the log line was emitted
 * @param level     log severity
 * @param logger    originating logger name (e.g. {@code com.foo.Bar})
 * @param thread    thread name that produced the line
 * @param message   the log message body
 */
public record LogIngestRequest(
        @NotNull(message = "serviceId is required") UUID serviceId,
        @NotNull(message = "timestamp is required") Instant timestamp,
        @NotNull(message = "level is required") LogLevel level,
        String logger,
        String thread,
        @NotBlank(message = "message is required") String message) {
}

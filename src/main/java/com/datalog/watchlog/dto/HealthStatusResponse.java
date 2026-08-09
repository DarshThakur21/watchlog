package com.datalog.watchlog.dto;

import com.datalog.watchlog.model.enums.ServiceStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Current health status of a single service, for the dashboard.
 *
 * @param serviceId     the service this status belongs to
 * @param status        latest observed status
 * @param lastCheckedAt when the status was last polled
 * @param responseTimeMs latest observed response time (nullable)
 */
public record HealthStatusResponse(
        UUID serviceId,
        ServiceStatus status,
        Instant lastCheckedAt,
        Long responseTimeMs) {
}

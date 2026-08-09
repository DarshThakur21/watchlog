package com.datalog.watchlog.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response body for a {@code Service}. Deliberately omits {@code apiKey} so it is
 * never echoed back over the wire.
 *
 * @param id                  generated database id
 * @param projectId           id of the owning project
 * @param serviceName         display name of the service
 * @param baseUrl             base URL used by the health poller
 * @param healthCheckEndpoint relative health-check path
 * @param createdAt           creation timestamp
 */
public record ServiceResponse(
        UUID id,
        UUID projectId,
        String serviceName,
        String baseUrl,
        String healthCheckEndpoint,
        Instant createdAt) {
}

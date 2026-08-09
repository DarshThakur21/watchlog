package com.datalog.watchlog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for creating a {@code Service}.
 *
 * @param projectId           id of the owning project
 * @param serviceName         display name of the service
 * @param baseUrl             base URL used by the health poller
 * @param healthCheckEndpoint relative health-check path (e.g. {@code /actuator/health})
 * @param apiKey              optional API key; auto-generated as a UUID if omitted
 */
public record ServiceRequest(
        @NotNull(message = "projectId is required") UUID projectId,
        @NotBlank(message = "serviceName is required") String serviceName,
        String baseUrl,
        String healthCheckEndpoint,
        String apiKey) {
}

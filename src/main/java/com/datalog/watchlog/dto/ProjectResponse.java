package com.datalog.watchlog.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response body for a {@code Project}. Never exposes internal entity state.
 *
 * @param id                 generated database id
 * @param projectName        unique display name of the project
 * @param projectDescription description of the project
 * @param createdAt          creation timestamp
 */
public record ProjectResponse(
        UUID id,
        String projectName,
        String projectDescription,
        Instant createdAt) {
}

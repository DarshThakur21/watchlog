package com.datalog.watchlog.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response body for a {@code Project}. Never exposes internal entity state.
 *
 * @param id        generated database id
 * @param name      unique display name of the project
 * @param createdAt creation timestamp
 */
public record ProjectResponse(
        UUID id,
        String name,
        Instant createdAt) {
}

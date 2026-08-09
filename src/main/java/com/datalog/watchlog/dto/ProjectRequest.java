package com.datalog.watchlog.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for creating a {@code Project}.
 *
 * @param name unique display name of the project
 */
public record ProjectRequest(
        @NotBlank(message = "name is required") String name) {
}

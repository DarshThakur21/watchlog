package com.datalog.watchlog.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for creating a {@code Project}.
 *
 * @param projectName        unique display name of the project
 * @param projectDescription optional description (stored as empty string if omitted)
 */
public record ProjectRequest(
        @NotBlank(message = "projectName is required") String projectName,
        String projectDescription) {
}

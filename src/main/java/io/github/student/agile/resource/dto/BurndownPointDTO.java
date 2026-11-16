package io.github.student.agile.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record BurndownPointDTO(
        Instant timestamp,
        @Schema(description = "Remaining work in story points") long remainingPoints) {
}

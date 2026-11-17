package io.github.student.agile.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record BurnupPointDTO(
        Instant timestamp,
        @Schema(description = "Completed work in story points") long completedPoints,
        @Schema(description = "Total sprint scope in story points") long totalPoints) {
}

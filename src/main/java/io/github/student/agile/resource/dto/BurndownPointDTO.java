package io.github.student.agile.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Single daily point on a sprint Burndown chart")
public class BurndownPointDTO {

    @Schema(description = "Point date on the chart timeline (UTC, ISO-8601 yyyy-MM-dd format)", example = "2026-11-10")
    private LocalDate date;

    @Schema(description = "Remaining work to complete (sum of uncompleted issue estimations) at the end of the given day (>= 0)", example = "13")
    private int remainingWork;

    @Schema(description = "Measurement unit of remaining work", example = "STORY_POINTS")
    private String unit;
}

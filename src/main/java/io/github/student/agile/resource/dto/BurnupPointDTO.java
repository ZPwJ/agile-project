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
@Schema(description = "Single daily point on a sprint Burnup chart")
public class BurnupPointDTO {

    @Schema(description = "Point date on the chart timeline (UTC, ISO-8601 yyyy-MM-dd format)", example = "2026-11-10")
    private LocalDate date;

    @Schema(description = "Cumulative sum of completed issue estimations (DONE status) at the end of the given day (>= 0)", example = "8")
    private int completedWork;

    @Schema(description = "Total estimation scope of all issues in the sprint at the end of the given day (>= 0)", example = "21")
    private int totalScope;

    @Schema(description = "Measurement unit of completed work and total scope", example = "STORY_POINTS")
    private String unit;
}

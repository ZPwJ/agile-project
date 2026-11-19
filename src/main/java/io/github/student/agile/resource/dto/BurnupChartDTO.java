package io.github.student.agile.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Sprint Burnup chart data showing cumulative completed work relative to total sprint scope")
public class BurnupChartDTO {

    @Schema(description = "Sprint identifier", example = "1")
    private Integer sprintId;

    @Schema(description = "Sprint name", example = "Sprint 1")
    private String sprintName;

    @Schema(description = "Project identifier", example = "10")
    private Integer projectId;

    @Schema(description = "Sprint start timestamp", nullable = true)
    private ZonedDateTime startDate;

    @Schema(description = "Sprint end timestamp (null for an active sprint)", nullable = true)
    private ZonedDateTime endDate;

    @Schema(description = "Measurement unit for all chart series values", example = "STORY_POINTS")
    private String unit;

    @Schema(description = "Chronologically ordered daily series of Burnup chart points")
    private List<BurnupPointDTO> points;
}

package io.github.student.agile.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Summary of a single sprint in the project comparison report")
public class SprintComparisonItemDTO {

    @Schema(description = "Project identifier", example = "10")
    private Integer projectId;

    @Schema(description = "Sprint identifier", example = "1")
    private Integer sprintId;

    @Schema(description = "Sprint name", example = "Sprint 1")
    private String sprintName;

    @Schema(description = "Sprint start timestamp", nullable = true)
    private ZonedDateTime startDate;

    @Schema(description = "Sprint end timestamp (null for an active sprint)", nullable = true)
    private ZonedDateTime endDate;

    @Schema(description = "Number of completed issues (DONE status)", example = "4")
    private int completedIssueCount;

    @Schema(description = "Number of uncompleted issues in the sprint", example = "1")
    private int uncompletedIssueCount;

    @Schema(description = "Sprint velocity in Story Points (0 when there are no completed issues or estimations)", example = "13")
    private int velocity;
}

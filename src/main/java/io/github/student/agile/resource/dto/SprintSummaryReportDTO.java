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
@Schema(description = "Summary report for a single sprint")
public class SprintSummaryReportDTO {

    @Schema(description = "Sprint identifier", example = "1")
    private Integer sprintId;

    @Schema(description = "Sprint name", example = "Sprint 1")
    private String sprintName;

    @Schema(description = "Project identifier", example = "10")
    private Integer projectId;

    @Schema(description = "Sprint start timestamp", nullable = true)
    private ZonedDateTime startDate;

    @Schema(description = "Sprint end timestamp (null for an active or unstarted sprint)", nullable = true)
    private ZonedDateTime endDate;

    @Schema(description = "Number of issues in the sprint at the time it started", example = "5")
    private int initialIssueCount;

    @Schema(description = "Number of completed issues (DONE status)", example = "3")
    private int completedIssueCount;

    @Schema(description = "Number of uncompleted issues in the sprint", example = "2")
    private int uncompletedIssueCount;

    @Schema(description = "Sprint velocity (sum of estimations of completed issues; missing estimation counts as 0)", example = "13")
    private int velocity;

    @Schema(description = "Sum of estimations of uncompleted issues (missing estimation counts as 0)", example = "8")
    private int uncompletedEstimationSum;

    @Schema(description = "Estimation unit used in metrics", example = "STORY_POINTS")
    private String estimationUnit;
}

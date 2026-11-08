package io.github.student.agile.resource.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Summary of a single sprint (WF-1). All estimation sums are expressed in {@link #estimationUnit}.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SprintReportDTO {

    public static final String STORY_POINTS = "STORY_POINTS";

    private Integer sprintID;
    private String sprintName;
    private Integer projectID;
    private SprintState state;
    private ZonedDateTime plannedStartDate;
    private ZonedDateTime startedAt;
    private ZonedDateTime endedAt;
    /** False when the sprint has no recorded history and the report reflects the current state of its issues. */
    private boolean historyAvailable;
    private int initialIssueCount;
    private int completedIssueCount;
    private int uncompletedIssueCount;
    private int velocity;
    private int uncompletedEstimation;
    private String estimationUnit;
}

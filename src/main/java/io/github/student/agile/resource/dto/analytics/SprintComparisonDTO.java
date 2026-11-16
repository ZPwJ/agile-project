package io.github.student.agile.resource.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Comparison of the finished sprints of one project (WF-5).
 * <p>
 * {@link #sprints} are ordered from the oldest to the newest: by {@code startedAt} ascending, sprints without
 * {@code startedAt} (finished before the analytics module recorded history) first, ties broken by sprint ID.
 * Metrics follow the rules of the sprint report and are expressed in {@link #estimationUnit}.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SprintComparisonDTO {

    private Integer projectID;
    private String estimationUnit;
    private List<SprintResult> sprints;

    /**
     * @param historyAvailable false when the sprint has no recorded events and the metrics reflect its current issues
     * @param velocity         sum of estimations of completed issues, taken at the moment of completion
     */
    public record SprintResult(
            Integer sprintID,
            String sprintName,
            ZonedDateTime startedAt,
            ZonedDateTime endedAt,
            boolean historyAvailable,
            int completedIssueCount,
            int uncompletedIssueCount,
            int velocity) {
    }
}

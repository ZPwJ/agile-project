package io.github.student.agile.resource.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Data points of the burnup chart of a sprint (WF-4).
 * <p>
 * {@link #points} are ordered chronologically. Point {@code day = 0} is the moment the sprint started,
 * point {@code day = n} the end of the n-th calendar day of the sprint in {@link #timeZone}.
 * Work is expressed in {@link #estimationUnit}. When {@link #historyAvailable} is false the list is empty.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SprintBurnupDTO {

    private Integer sprintID;
    private String sprintName;
    private Integer projectID;
    private SprintState state;
    private ZonedDateTime startedAt;
    private ZonedDateTime endedAt;
    private boolean historyAvailable;
    private String estimationUnit;
    private String timeZone;
    private List<Point> points;

    /**
     * @param completedWork cumulative estimation of issues completed in this sprint until this point
     * @param totalScope    estimation of all issues in the sprint at this point, changed by issues added,
     *                      removed or re-estimated during the sprint
     */
    public record Point(int day, LocalDate date, int completedWork, int totalScope) {
    }
}

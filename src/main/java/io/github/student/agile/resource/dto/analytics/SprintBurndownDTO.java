package io.github.student.agile.resource.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Data points of the burndown chart of a sprint (WF-3).
 * <p>
 * {@link #points} are ordered chronologically. Point {@code day = 0} is the moment the sprint started,
 * point {@code day = n} the end of the n-th calendar day of the sprint in {@link #timeZone}.
 * Work is expressed in {@link #estimationUnit}. When {@link #historyAvailable} is false the list is empty.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SprintBurndownDTO {

    private Integer sprintID;
    private String sprintName;
    private Integer projectID;
    private SprintState state;
    private ZonedDateTime startedAt;
    private ZonedDateTime endedAt;
    private boolean historyAvailable;
    /** Sum of estimations of the issues in the sprint when it started; the burndown starts from this value. */
    private int initialScope;
    private String estimationUnit;
    private String timeZone;
    private List<Point> points;

    /**
     * @param remainingWork  initial scope minus the work completed until this point, never below 0
     * @param remainingScope current scope (including work added after the start) minus the work completed until this point
     */
    public record Point(int day, LocalDate date, int remainingWork, int remainingScope) {
    }
}

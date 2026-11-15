package io.github.student.agile.resource.service.analytics;

import java.time.LocalDate;
import java.util.List;

/**
 * Day-by-day progress of a sprint, the common source of the burndown (WF-3) and burnup (WF-4) charts.
 * All values are in story points.
 *
 * @param historyAvailable false when the sprint has no recorded events; {@code days} is then empty
 * @param initialScope     sum of estimations of the issues in the sprint when it started
 * @param days             chronological snapshots, see {@link DailyProgress}
 */
public record SprintProgress(
        boolean historyAvailable,
        int initialScope,
        List<DailyProgress> days) {

    public static SprintProgress withoutHistory() {
        return new SprintProgress(false, 0, List.of());
    }

    /**
     * State of the sprint at one point in time.
     *
     * @param day           0 for the moment the sprint started, then 1, 2, ... for the end of each calendar day
     * @param date          calendar day of the snapshot in the report time zone
     * @param completedWork estimations of issues completed so far and still in the sprint, taken at completion
     * @param totalScope    estimations of all issues in the sprint at that moment
     *                      (completed issues with their estimation at completion, others with the latest one)
     */
    public record DailyProgress(int day, LocalDate date, int completedWork, int totalScope) {
    }
}

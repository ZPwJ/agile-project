package io.github.student.agile.resource.analytics.metrics;

/**
 * Per-sprint numbers shared by the summary, comparison and export reports.
 * <p>
 * Rules: a task without an estimation counts in the task counts but adds 0 points.
 * {@code totalTasks} is the number of tasks in the sprint when the numbers were taken
 * (at stop time for a finished sprint, now otherwise); added/removed tasks are not tracked.
 *
 * @param completedEstimate   velocity: sum of estimations of completed tasks
 * @param incompleteEstimate  sum of estimations of not completed tasks
 * @param unestimatedTasks    tasks with no estimation (included in the task counts)
 */
public record SprintMetrics(Integer sprintId,
                            SprintState state,
                            int totalTasks,
                            int completedTasks,
                            int incompleteTasks,
                            int unestimatedTasks,
                            long completedEstimate,
                            long incompleteEstimate,
                            MetricUnit unit) {
}

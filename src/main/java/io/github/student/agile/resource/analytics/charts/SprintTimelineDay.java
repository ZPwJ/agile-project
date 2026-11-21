package io.github.student.agile.resource.analytics.charts;

import java.time.LocalDate;

/**
 * One point of the chart axis, one calendar day in {@code Europe/Warsaw}.
 *
 * @param completed           points finished on this day; may be negative when a task is reopened
 * @param cumulativeCompleted points finished by the end of this day
 */
public record SprintTimelineDay(LocalDate date, long completed, long cumulativeCompleted) {
}

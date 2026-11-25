package io.github.student.agile.resource.dto.analytics;

import lombok.Builder;

import java.time.LocalDate;

/**
 * Burndown/burnup chart data point at the end of a given sprint day.
 *
 * @param date          sprint day (ISO-8601, YYYY-MM-DD)
 * @param remainingWork remaining work (never below 0) - burndown Y axis
 * @param completedWork cumulative completed work - burnup Y axis
 * @param totalScope    total sprint scope on that day - burnup scope line
 */
@Builder
public record ChartPointDTO(
        LocalDate date,
        int remainingWork,
        int completedWork,
        int totalScope
) {
}

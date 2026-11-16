package io.github.student.agile.resource.service;

import io.github.student.agile.resource.dto.analytics.SprintComparisonDTO;

import java.time.LocalDate;

public interface SprintComparisonService {

    /**
     * @param from first day of the date range (inclusive), {@code null} for no lower bound
     * @param to   last day of the date range (inclusive), {@code null} for no upper bound
     */
    SprintComparisonDTO compareSprints(Integer projectID, LocalDate from, LocalDate to);
}

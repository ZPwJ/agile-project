package io.github.student.agile.resource.analytics;

import io.github.student.agile.resource.dto.analytics.SprintComparisonRowDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * Comparison of the sprints of a single project, also used by the CSV export.
 */
public interface SprintComparisonService {

    /**
     * @param projectId project identifier
     * @param from      start of the date range (inclusive) or {@code null} for no lower bound
     * @param to        end of the date range (inclusive) or {@code null} for no upper bound
     * @return comparison rows in a documented, stable order (oldest sprint first)
     */
    List<SprintComparisonRowDTO> getProjectSprints(Integer projectId, LocalDate from, LocalDate to);
}

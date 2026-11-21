package io.github.student.agile.resource.analytics.metrics;

import io.github.student.agile.resource.analytics.error.SprintNotFoundException;

import java.util.Collection;
import java.util.Map;

/** Single source of truth for sprint numbers. Reports must not recompute these in controllers. */
public interface SprintMetricsProvider {

    /** @throws SprintNotFoundException when the sprint does not exist */
    SprintMetrics forSprint(Integer sprintId);

    /**
     * Batch variant (no per-sprint queries) for comparison reports.
     *
     * @return metrics keyed by sprint id
     * @throws SprintNotFoundException when any of the sprints does not exist
     */
    Map<Integer, SprintMetrics> forSprints(Collection<Integer> sprintIds);
}

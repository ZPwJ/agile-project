package io.github.student.agile.resource.analytics.charts;

import io.github.student.agile.resource.dto.BurndownDTO;
import io.github.student.agile.resource.dto.BurndownPointDTO;
import io.github.student.agile.resource.dto.BurnupDTO;
import io.github.student.agile.resource.dto.BurnupPointDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Maps the chart timeline to the WF-3/WF-4 DTOs.
 * No counting logic, no repository access and no entity access: the timeline owns the rules.
 */
@Service
@RequiredArgsConstructor
public class SprintChartsService {

    private final SprintTimelineService timelineService;

    public BurndownDTO burndown(Integer sprintId) {
        SprintTimeline timeline = timelineService.forSprint(sprintId);
        return new BurndownDTO(timeline.sprintId(), timeline.unit(), timeline.dataCompleteness(),
                timeline.warnings(),
                timeline.days().stream()
                        .map(day -> new BurndownPointDTO(day.date(), remaining(timeline, day)))
                        .toList());
    }

    public BurnupDTO burnup(Integer sprintId) {
        SprintTimeline timeline = timelineService.forSprint(sprintId);
        return new BurnupDTO(timeline.sprintId(), timeline.unit(), timeline.dataCompleteness(),
                timeline.warnings(),
                timeline.days().stream()
                        // The scope is constant on every point; SCOPE_NOT_HISTORICAL explains why (E12).
                        .map(day -> new BurnupPointDTO(day.date(), day.cumulativeCompleted(),
                                timeline.initialScope()))
                        .toList());
    }

    private static long remaining(SprintTimeline timeline, SprintTimelineDay day) {
        // RULE E13: burndown never goes below zero.
        return Math.max(0L, timeline.initialScope() - day.cumulativeCompleted());
    }
}

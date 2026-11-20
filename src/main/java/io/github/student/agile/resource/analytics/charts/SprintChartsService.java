package io.github.student.agile.resource.analytics.charts;

import io.github.student.agile.resource.dto.BurndownDTO;
import io.github.student.agile.resource.dto.BurndownPointDTO;
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
                        // RULE E13: burndown never goes below zero.
                        .map(day -> new BurndownPointDTO(day.date(),
                                Math.max(0L, timeline.initialScope() - day.cumulativeCompleted())))
                        .toList());
    }
}

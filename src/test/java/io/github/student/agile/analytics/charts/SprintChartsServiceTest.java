package io.github.student.agile.analytics.charts;

import io.github.student.agile.resource.analytics.charts.SprintChartDataCompleteness;
import io.github.student.agile.resource.analytics.charts.SprintChartsService;
import io.github.student.agile.resource.analytics.charts.SprintTimeline;
import io.github.student.agile.resource.analytics.charts.SprintTimelineDay;
import io.github.student.agile.resource.analytics.charts.SprintTimelineService;
import io.github.student.agile.resource.analytics.metrics.MetricUnit;
import io.github.student.agile.resource.dto.BurndownDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Chart DTO mapping edge cases that cannot be produced from real data (plan rule E13). */
public class SprintChartsServiceTest {

    @Test
    public void burndown_cumulativeCompletedAboveScope_remainingIsClampedAtZero() {
        // Hand-built timeline: more points completed than the scope, which real data cannot produce.
        SprintTimeline timeline = new SprintTimeline(7, MetricUnit.STORY_POINTS, 5,
                List.of(new SprintTimelineDay(LocalDate.of(2026, 11, 2), 3, 8)),
                SprintChartDataCompleteness.COMPLETE,
                List.of());
        SprintTimelineService timelineService = mock(SprintTimelineService.class);
        when(timelineService.forSprint(7)).thenReturn(timeline);

        BurndownDTO burndown = new SprintChartsService(timelineService).burndown(7);

        // RULE E13: burndown never goes below zero.
        assertEquals(0L, burndown.points().get(0).remaining());
    }
}

package io.github.student.agile.analytics.charts;

import io.github.student.agile.resource.analytics.charts.SprintChartDataCompleteness;
import io.github.student.agile.resource.analytics.charts.SprintChartWarning;
import io.github.student.agile.resource.analytics.charts.SprintChartsService;
import io.github.student.agile.resource.analytics.charts.SprintTimeline;
import io.github.student.agile.resource.analytics.charts.SprintTimelineDay;
import io.github.student.agile.resource.analytics.charts.SprintTimelineService;
import io.github.student.agile.resource.analytics.metrics.MetricUnit;
import io.github.student.agile.resource.dto.BurndownDTO;
import io.github.student.agile.resource.dto.BurnupDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Plain JUnit tests of the chart DTO mapping and the E13 clamp, using hand-built timelines. */
public class SprintChartsServiceTest {

    @Test
    public void burndown_cumulativeCompletedAboveScope_remainingIsClampedAtZero() {
        // Hand-built timeline: more points completed than the scope, which real data cannot produce.
        SprintTimeline timeline = new SprintTimeline(7, MetricUnit.STORY_POINTS, 5,
                List.of(new SprintTimelineDay(LocalDate.of(2026, 11, 2), 3, 8)),
                SprintChartDataCompleteness.COMPLETE,
                List.of());

        BurndownDTO burndown = serviceFor(timeline).burndown(7);

        // RULE E13: burndown never goes below zero.
        assertEquals(0L, burndown.points().get(0).remaining());
    }

    @Test
    public void burndown_mapsTimelineDaysToRemainingPoints() {
        SprintTimeline timeline = new SprintTimeline(7, MetricUnit.STORY_POINTS, 8,
                List.of(
                        new SprintTimelineDay(LocalDate.of(2026, 11, 2), 0, 0),
                        new SprintTimelineDay(LocalDate.of(2026, 11, 3), 5, 5)),
                SprintChartDataCompleteness.COMPLETE,
                List.of(SprintChartWarning.SCOPE_NOT_HISTORICAL.render()));

        BurndownDTO burndown = serviceFor(timeline).burndown(7);

        assertEquals(7, burndown.sprintId());
        assertEquals(MetricUnit.STORY_POINTS, burndown.unit());
        assertEquals(SprintChartDataCompleteness.COMPLETE, burndown.dataCompleteness());
        assertEquals(timeline.warnings(), burndown.warnings());
        assertEquals(2, burndown.points().size());
        assertEquals(LocalDate.of(2026, 11, 2), burndown.points().get(0).date());
        assertEquals(8L, burndown.points().get(0).remaining());
        assertEquals(3L, burndown.points().get(1).remaining());
    }

    @Test
    public void burnup_mapsCumulativeCompletedAndConstantScope() {
        SprintTimeline timeline = new SprintTimeline(7, MetricUnit.STORY_POINTS, 8,
                List.of(
                        new SprintTimelineDay(LocalDate.of(2026, 11, 2), 5, 5),
                        new SprintTimelineDay(LocalDate.of(2026, 11, 3), 3, 8)),
                SprintChartDataCompleteness.PARTIAL,
                List.of(SprintChartWarning.UNDATED_COMPLETION.render(1)));

        BurnupDTO burnup = serviceFor(timeline).burnup(7);

        assertEquals(7, burnup.sprintId());
        assertEquals(MetricUnit.STORY_POINTS, burnup.unit());
        assertEquals(SprintChartDataCompleteness.PARTIAL, burnup.dataCompleteness());
        assertEquals(timeline.warnings(), burnup.warnings());
        assertEquals(2, burnup.points().size());
        assertEquals(LocalDate.of(2026, 11, 2), burnup.points().get(0).date());
        // Burnup uses the cumulative value, not the day's delta, and the scope never changes.
        assertEquals(5L, burnup.points().get(0).completed());
        assertEquals(8L, burnup.points().get(0).scope());
        assertEquals(8L, burnup.points().get(1).completed());
        assertEquals(8L, burnup.points().get(1).scope());
    }

    private static SprintChartsService serviceFor(SprintTimeline timeline) {
        SprintTimelineService timelineService = mock(SprintTimelineService.class);
        when(timelineService.forSprint(timeline.sprintId())).thenReturn(timeline);
        return new SprintChartsService(timelineService);
    }
}

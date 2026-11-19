package io.github.student.agile.service.analytics;

import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintIssueEvent;
import io.github.student.agile.resource.domain.dictionary.SprintIssueEventType;
import io.github.student.agile.resource.service.analytics.SprintMetricsCalculator;
import io.github.student.agile.resource.service.analytics.SprintProgress;
import io.github.student.agile.resource.service.analytics.SprintProgress.DailyProgress;
import io.github.student.agile.resource.service.analytics.SprintProgressCalculator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;

import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.ADDED;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.COMPLETED;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.ESTIMATION_CHANGED;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.REMOVED;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.SCOPE_AT_START;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SprintProgressCalculatorTest {

    private static final Integer SPRINT_ID = 1;
    /** Monday 9:00 in Warsaw (UTC+1 in November). */
    private static final ZonedDateTime STARTED_AT = ZonedDateTime.parse("2026-11-02T09:00:00+01:00");
    private static final LocalDate FIRST_DAY = LocalDate.parse("2026-11-02");

    private final SprintMetricsCalculator metricsCalculator = new SprintMetricsCalculator();
    private final SprintProgressCalculator calculator = new SprintProgressCalculator(metricsCalculator);
    private int sequence = 0;

    @Test
    void calculate_sprintWithoutHistory_returnsNoDays() {

        Sprint sprint = Sprint.builder().id(SPRINT_ID).sprintIssues(new HashSet<>()).active(false).isDone(false).build();

        SprintProgress progress = calculator.calculate(sprint, List.of(), STARTED_AT);

        assertFalse(progress.historyAvailable());
        assertEquals(0, progress.initialScope());
        assertTrue(progress.days().isEmpty());
    }

    @Test
    void calculate_finishedSprint_returnsStartPointAndOnePointPerDay() {

        List<SprintIssueEvent> events = List.of(
                event(1, SCOPE_AT_START, 5, STARTED_AT),
                event(2, SCOPE_AT_START, 8, STARTED_AT),
                event(3, SCOPE_AT_START, 3, STARTED_AT),
                event(1, COMPLETED, 5, STARTED_AT.plusHours(3)),
                event(2, COMPLETED, 8, STARTED_AT.plusDays(2)));

        SprintProgress progress = calculator.calculate(finishedSprint(STARTED_AT.plusDays(3)), events, STARTED_AT.plusDays(30));

        assertTrue(progress.historyAvailable());
        assertEquals(16, progress.initialScope());
        assertEquals(List.of(
                new DailyProgress(0, FIRST_DAY, 0, 16),
                new DailyProgress(1, FIRST_DAY, 5, 16),
                new DailyProgress(2, FIRST_DAY.plusDays(1), 5, 16),
                new DailyProgress(3, FIRST_DAY.plusDays(2), 13, 16),
                new DailyProgress(4, FIRST_DAY.plusDays(3), 13, 16)), progress.days());
    }

    @Test
    void calculate_daysAreChronologicalAndConsecutive() {

        SprintProgress progress = calculator.calculate(finishedSprint(STARTED_AT.plusDays(9)),
                List.of(event(1, SCOPE_AT_START, 1, STARTED_AT)), STARTED_AT.plusDays(30));

        List<DailyProgress> days = progress.days();
        assertEquals(11, days.size());
        for (int i = 1; i < days.size(); i++) {
            assertEquals(i, days.get(i).day());
            assertEquals(FIRST_DAY.plusDays(i - 1), days.get(i).date());
        }
    }

    @Test
    void calculate_activeSprint_endsToday() {

        List<SprintIssueEvent> events = List.of(
                event(1, SCOPE_AT_START, 5, STARTED_AT),
                event(1, COMPLETED, 5, STARTED_AT.plusDays(1)));

        SprintProgress progress = calculator.calculate(activeSprint(), events, STARTED_AT.plusDays(1).plusHours(2));

        assertEquals(3, progress.days().size());
        DailyProgress today = progress.days().get(2);
        assertEquals(FIRST_DAY.plusDays(1), today.date());
        assertEquals(5, today.completedWork());
    }

    @Test
    void calculate_scopeChanges_changeTotalScopeButNotInitialScope() {

        List<SprintIssueEvent> events = List.of(
                event(1, SCOPE_AT_START, 5, STARTED_AT),
                event(2, SCOPE_AT_START, 3, STARTED_AT),
                event(3, ADDED, 8, STARTED_AT.plusDays(1)),
                event(2, REMOVED, 3, STARTED_AT.plusDays(1)),
                event(1, ESTIMATION_CHANGED, 2, STARTED_AT.plusDays(2)));

        SprintProgress progress = calculator.calculate(finishedSprint(STARTED_AT.plusDays(2)), events, STARTED_AT.plusDays(30));

        assertEquals(8, progress.initialScope());
        assertEquals(List.of(8, 8, 13, 10), progress.days().stream().map(DailyProgress::totalScope).toList());
        assertEquals(List.of(0, 0, 0, 0), progress.days().stream().map(DailyProgress::completedWork).toList());
    }

    @Test
    void calculate_issueRemovedAfterCompletion_isNoLongerCompletedWork() {

        List<SprintIssueEvent> events = List.of(
                event(1, SCOPE_AT_START, 5, STARTED_AT),
                event(1, COMPLETED, 5, STARTED_AT.plusHours(1)),
                event(1, REMOVED, 5, STARTED_AT.plusDays(1)));

        SprintProgress progress = calculator.calculate(finishedSprint(STARTED_AT.plusDays(1)), events, STARTED_AT.plusDays(30));

        assertEquals(List.of(0, 5, 0), progress.days().stream().map(DailyProgress::completedWork).toList());
        assertEquals(List.of(5, 5, 0), progress.days().stream().map(DailyProgress::totalScope).toList());
    }

    @Test
    void calculate_estimationChangedAfterCompletion_keepsCompletedWorkAndScope() {

        List<SprintIssueEvent> events = List.of(
                event(1, SCOPE_AT_START, 5, STARTED_AT),
                event(1, COMPLETED, 5, STARTED_AT.plusHours(1)),
                event(1, ESTIMATION_CHANGED, 40, STARTED_AT.plusDays(1)));

        SprintProgress progress = calculator.calculate(finishedSprint(STARTED_AT.plusDays(1)), events, STARTED_AT.plusDays(30));

        DailyProgress lastDay = progress.days().get(progress.days().size() - 1);
        assertEquals(5, lastDay.completedWork());
        assertEquals(5, lastDay.totalScope());
    }

    @Test
    void calculate_issuesWithoutEstimation_addNoWork() {

        List<SprintIssueEvent> events = List.of(
                event(1, SCOPE_AT_START, null, STARTED_AT),
                event(2, SCOPE_AT_START, 2, STARTED_AT),
                event(1, COMPLETED, null, STARTED_AT.plusHours(1)));

        SprintProgress progress = calculator.calculate(finishedSprint(STARTED_AT.plusHours(2)), events, STARTED_AT.plusDays(30));

        assertEquals(2, progress.initialScope());
        assertEquals(List.of(new DailyProgress(0, FIRST_DAY, 0, 2), new DailyProgress(1, FIRST_DAY, 0, 2)), progress.days());
    }

    @Test
    void calculate_sprintWithoutEstimatedIssues_returnsZeroPoints() {

        List<SprintIssueEvent> events = List.of(
                event(1, SCOPE_AT_START, null, STARTED_AT),
                event(1, COMPLETED, null, STARTED_AT.plusHours(1)));

        SprintProgress progress = calculator.calculate(finishedSprint(STARTED_AT.plusDays(1)), events, STARTED_AT.plusDays(30));

        assertTrue(progress.historyAvailable());
        assertEquals(0, progress.initialScope());
        assertEquals(3, progress.days().size());
        assertTrue(progress.days().stream().allMatch(day -> day.completedWork() == 0 && day.totalScope() == 0));
    }

    @Test
    void calculate_daysFollowWarsawTimeZone() {

        // 23:30 UTC on 2 November is already 00:30 on 3 November in Warsaw.
        List<SprintIssueEvent> events = List.of(
                event(1, SCOPE_AT_START, 5, STARTED_AT),
                event(1, COMPLETED, 5, ZonedDateTime.parse("2026-11-02T23:30:00Z")));

        SprintProgress progress = calculator.calculate(finishedSprint(STARTED_AT.plusDays(1)), events, STARTED_AT.plusDays(30));

        assertEquals(List.of(0, 0, 5), progress.days().stream().map(DailyProgress::completedWork).toList());
    }

    @Test
    void calculate_endBeforeStart_returnsOnlyFirstDay() {

        SprintProgress progress = calculator.calculate(finishedSprint(STARTED_AT.minusDays(2)),
                List.of(event(1, SCOPE_AT_START, 5, STARTED_AT)), STARTED_AT);

        assertEquals(2, progress.days().size());
        assertEquals(FIRST_DAY, progress.days().get(1).date());
    }

    @Test
    void calculate_lastDayCompletedWork_equalsVelocity() {

        List<SprintIssueEvent> events = List.of(
                event(1, SCOPE_AT_START, 5, STARTED_AT),
                event(2, SCOPE_AT_START, 3, STARTED_AT),
                event(3, ADDED, 8, STARTED_AT.plusDays(1)),
                event(3, COMPLETED, 8, STARTED_AT.plusDays(2)),
                event(1, ESTIMATION_CHANGED, 13, STARTED_AT.plusDays(2)),
                event(1, COMPLETED, 13, STARTED_AT.plusDays(3)));
        Sprint sprint = finishedSprint(STARTED_AT.plusDays(4));

        SprintProgress progress = calculator.calculate(sprint, events, STARTED_AT.plusDays(30));

        DailyProgress lastDay = progress.days().get(progress.days().size() - 1);
        var metrics = metricsCalculator.calculate(sprint, events);
        assertEquals(metrics.velocity(), lastDay.completedWork());
        assertEquals(metrics.velocity() + metrics.uncompletedEstimation(), lastDay.totalScope());
    }

    private static Sprint activeSprint() {
        return Sprint.builder().id(SPRINT_ID).sprintIssues(new HashSet<>()).startedAt(STARTED_AT).active(true).isDone(false).build();
    }

    private static Sprint finishedSprint(ZonedDateTime endedAt) {
        return Sprint.builder().id(SPRINT_ID).sprintIssues(new HashSet<>())
                .startedAt(STARTED_AT).endedAt(endedAt).active(false).isDone(true).build();
    }

    private SprintIssueEvent event(Integer issueID, SprintIssueEventType type, Integer estimation, ZonedDateTime occurredAt) {
        sequence++;
        return new SprintIssueEvent(sequence, SPRINT_ID, issueID, type, estimation, occurredAt);
    }
}

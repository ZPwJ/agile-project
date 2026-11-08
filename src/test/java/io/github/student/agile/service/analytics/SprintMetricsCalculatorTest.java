package io.github.student.agile.service.analytics;

import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.IssueStatus;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintIssueEvent;
import io.github.student.agile.resource.domain.dictionary.IssueStatusEnum;
import io.github.student.agile.resource.domain.dictionary.IssueStatusID;
import io.github.student.agile.resource.domain.dictionary.SprintIssueEventType;
import io.github.student.agile.resource.service.analytics.SprintMetrics;
import io.github.student.agile.resource.service.analytics.SprintMetricsCalculator;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.DONE;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.IN_TESTS;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.ADDED;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.COMPLETED;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.ESTIMATION_CHANGED;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.REMOVED;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.SCOPE_AT_START;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SprintMetricsCalculatorTest {

    private static final Integer SPRINT_ID = 1;
    private static final ZonedDateTime STARTED_AT = ZonedDateTime.parse("2026-11-02T09:00:00+01:00");

    private final SprintMetricsCalculator calculator = new SprintMetricsCalculator();
    private int sequence = 0;

    @Test
    void velocity_plannedSprintWithoutIssues_isZero() {

        SprintMetrics metrics = calculator.calculate(plannedSprint(new HashSet<>()), List.of());

        assertFalse(metrics.historyAvailable());
        assertEquals(new SprintMetrics(false, 0, 0, 0, 0, 0), metrics);
    }

    @Test
    void velocity_plannedSprintWithNullIssueSet_isZero() {

        SprintMetrics metrics = calculator.calculate(plannedSprint(null), List.of());

        assertEquals(0, metrics.velocity());
        assertEquals(0, metrics.initialIssueCount());
    }

    @Test
    void velocity_startedSprintWithoutRemainingIssues_isZero() {

        SprintMetrics metrics = calculator.calculate(startedSprint(), List.of(
                event(1, SCOPE_AT_START, 5),
                event(1, REMOVED, 5)));

        assertTrue(metrics.historyAvailable());
        assertEquals(new SprintMetrics(true, 1, 0, 0, 0, 0), metrics);
    }

    @Test
    void velocity_onlyUncompletedIssues_isZero() {

        SprintMetrics metrics = calculator.calculate(startedSprint(), List.of(
                event(1, SCOPE_AT_START, 5),
                event(2, SCOPE_AT_START, 3),
                event(3, ADDED, 8)));

        assertEquals(0, metrics.velocity());
        assertEquals(0, metrics.completedIssueCount());
        assertEquals(3, metrics.uncompletedIssueCount());
        assertEquals(16, metrics.uncompletedEstimation());
    }

    @Test
    void velocity_sumsOnlyCompletedIssues() {

        SprintMetrics metrics = calculator.calculate(startedSprint(), List.of(
                event(1, SCOPE_AT_START, 5),
                event(2, SCOPE_AT_START, 3),
                event(3, SCOPE_AT_START, 8),
                event(1, COMPLETED, 5),
                event(3, COMPLETED, 8)));

        assertEquals(13, metrics.velocity());
        assertEquals(2, metrics.completedIssueCount());
        assertEquals(1, metrics.uncompletedIssueCount());
        assertEquals(3, metrics.uncompletedEstimation());
    }

    @Test
    void velocity_completedIssuesWithoutEstimation_areCountedButAddNoPoints() {

        SprintMetrics metrics = calculator.calculate(startedSprint(), List.of(
                event(1, SCOPE_AT_START, null),
                event(2, SCOPE_AT_START, 2),
                event(3, SCOPE_AT_START, null),
                event(1, COMPLETED, null),
                event(2, COMPLETED, 2)));

        assertEquals(2, metrics.velocity());
        assertEquals(2, metrics.completedIssueCount());
        assertEquals(1, metrics.uncompletedIssueCount());
        assertEquals(0, metrics.uncompletedEstimation());
    }

    @Test
    void velocity_usesEstimationAtCompletion() {

        SprintMetrics metrics = calculator.calculate(startedSprint(), List.of(
                event(1, SCOPE_AT_START, 3),
                event(1, ESTIMATION_CHANGED, 5),
                event(1, COMPLETED, 5),
                event(1, ESTIMATION_CHANGED, 40)));

        assertEquals(5, metrics.velocity());
    }

    @Test
    void velocity_issueRemovedAfterCompletion_isNotCounted() {

        SprintMetrics metrics = calculator.calculate(startedSprint(), List.of(
                event(1, SCOPE_AT_START, 5),
                event(2, SCOPE_AT_START, 3),
                event(1, COMPLETED, 5),
                event(2, COMPLETED, 3),
                event(1, REMOVED, 5)));

        assertEquals(3, metrics.velocity());
        assertEquals(1, metrics.completedIssueCount());
    }

    @Test
    void velocity_sprintWithoutHistory_usesCurrentDoneIssues() {

        Sprint sprint = plannedSprint(Set.of(
                issue(1, 5, DONE),
                issue(2, 3, IN_TESTS),
                issue(3, null, DONE)));

        SprintMetrics metrics = calculator.calculate(sprint, List.of(event(9, COMPLETED, 100)));

        assertFalse(metrics.historyAvailable());
        assertEquals(5, metrics.velocity());
        assertEquals(2, metrics.completedIssueCount());
        assertEquals(1, metrics.uncompletedIssueCount());
        assertEquals(3, metrics.uncompletedEstimation());
    }

    private static Sprint plannedSprint(Set<Issue> issues) {
        return Sprint.builder().id(SPRINT_ID).sprintIssues(issues).active(false).isDone(false).build();
    }

    private static Sprint startedSprint() {
        return Sprint.builder().id(SPRINT_ID).sprintIssues(new HashSet<>()).startedAt(STARTED_AT).active(true).isDone(false).build();
    }

    private static Issue issue(Integer id, Integer estimation, IssueStatusID status) {
        IssueStatus issueStatus = new IssueStatus(status.getStatusID(), IssueStatusEnum.valueOf(status.name()));
        return Issue.builder().id(id).estimation(estimation).issueStatus(issueStatus).build();
    }

    private SprintIssueEvent event(Integer issueID, SprintIssueEventType type, Integer estimation) {
        sequence++;
        return new SprintIssueEvent(sequence, SPRINT_ID, issueID, type, estimation, STARTED_AT.plusMinutes(sequence));
    }
}

package io.github.student.agile.analytics;

import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.IssueStatus;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.dictionary.IssueStatusID;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.DONE;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.IN_PROGRESS;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.TO_DO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SprintBurndownTest {

    private final SprintMetricsCalculator calculator = new SprintMetricsCalculator();
    private final ZoneId zone = ZoneId.of("Europe/Warsaw");

    private final ZonedDateTime start = ZonedDateTime.of(2026, 11, 1, 9, 0, 0, 0, zone);
    private final ZonedDateTime end = ZonedDateTime.of(2026, 11, 5, 17, 0, 0, 0, zone);

    private final Sprint sprint = Sprint.builder()
            .id(1)
            .name("Sprint 1")
            .startDate(start)
            .endDate(end)
            .active(false)
            .isDone(false)
            .build();

    @Test
    void pointsAreInStrictChronologicalOrder() {
        List<BurndownPoint> points = calculator.calculateBurndown(sprint, List.of(issue(DONE, 5, 2)));

        assertEquals(5, points.size());
        assertEquals(LocalDate.of(2026, 11, 1), points.get(0).date());
        assertEquals(LocalDate.of(2026, 11, 2), points.get(1).date());
        assertEquals(LocalDate.of(2026, 11, 3), points.get(2).date());
        assertEquals(LocalDate.of(2026, 11, 4), points.get(3).date());
        assertEquals(LocalDate.of(2026, 11, 5), points.get(4).date());

        for (int i = 0; i < points.size() - 1; i++) {
            assertTrue(points.get(i).date().isBefore(points.get(i + 1).date()),
                    "Point " + i + " should be before point " + (i + 1));
        }
    }

    @Test
    void remainingWorkValuesAreAlwaysNonNegative() {
        Issue overCompleted = issue(DONE, 20, 1);
        Sprint sprintWithSmallScope = Sprint.builder()
                .id(2)
                .name("Small Sprint")
                .startDate(start)
                .endDate(end)
                .active(false)
                .isDone(false)
                .build();
        overCompleted.setSprint(sprintWithSmallScope);

        List<BurndownPoint> points = calculator.calculateBurndown(sprintWithSmallScope, List.of(overCompleted));

        for (BurndownPoint point : points) {
            assertTrue(point.remainingWork() >= 0, "Remaining work must be >= 0, was " + point.remainingWork());
            assertTrue(point.remaining() >= 0, "Remaining accessor must be >= 0");
            assertEquals("STORY_POINTS", point.unit());
        }
    }

    @Test
    void sprintWithoutEstimationHasZeroRemainingWorkForEveryDay() {
        List<Issue> issues = List.of(
                issue(DONE, null, 2),
                issue(DONE, 0, 3),
                issue(IN_PROGRESS, null, null),
                issue(TO_DO, 0, null));

        List<BurndownPoint> points = calculator.calculateBurndown(sprint, issues);

        assertEquals(5, points.size());
        for (BurndownPoint point : points) {
            assertEquals(0, point.remainingWork());
            assertEquals("STORY_POINTS", point.unit());
        }
    }

    @Test
    void severalDoneTasksOnDifferentDaysBurnDownProgressively() {
        // Total scope = 3 + 5 + 7 = 15 SP.
        // Task 1 (3 SP) DONE on Nov 2 (day 2)
        // Task 2 (5 SP) DONE on Nov 3 (day 3)
        // Task 3 (7 SP) DONE on Nov 5 (day 5)
        Issue task1 = issue(DONE, 3, 2);
        Issue task2 = issue(DONE, 5, 3);
        Issue task3 = issue(DONE, 7, 5);

        List<BurndownPoint> points = calculator.calculateBurndown(sprint, List.of(task1, task2, task3));

        assertEquals(5, points.size());
        // Nov 1: 0 completed -> 15 remaining
        assertEquals(15, points.get(0).remainingWork());
        // Nov 2: task1 completed -> 15 - 3 = 12 remaining
        assertEquals(12, points.get(1).remainingWork());
        // Nov 3: task1 + task2 completed -> 15 - 8 = 7 remaining
        assertEquals(7, points.get(2).remainingWork());
        // Nov 4: no task completed -> 7 remaining
        assertEquals(7, points.get(3).remainingWork());
        // Nov 5: task1 + task2 + task3 completed -> 15 - 15 = 0 remaining
        assertEquals(0, points.get(4).remainingWork());
    }

    @Test
    void activeSprintGeneratesPointsUpToToday() {
        LocalDate mockToday = LocalDate.of(2026, 11, 3);
        Sprint activeSprint = Sprint.builder()
                .id(3)
                .name("Active sprint")
                .startDate(start)
                .endDate(end)
                .active(true)
                .isDone(false)
                .build();
        Issue task = issue(DONE, 4, 2);
        task.setSprint(activeSprint);

        List<BurndownPoint> points = calculator.calculateBurndown(activeSprint, List.of(task), mockToday);

        assertEquals(3, points.size());
        assertEquals(LocalDate.of(2026, 11, 1), points.get(0).date());
        assertEquals(LocalDate.of(2026, 11, 2), points.get(1).date());
        assertEquals(LocalDate.of(2026, 11, 3), points.get(2).date());
        assertEquals(4, points.get(0).remainingWork());
        assertEquals(0, points.get(1).remainingWork());
        assertEquals(0, points.get(2).remainingWork());
    }

    @Test
    void emptySprintHasZeroRemainingWorkForEveryDay() {
        List<BurndownPoint> points = calculator.calculateBurndown(sprint, List.of());

        assertEquals(5, points.size());
        for (BurndownPoint point : points) {
            assertEquals(0, point.remainingWork());
            assertEquals("STORY_POINTS", point.unit());
        }
    }

    @Test
    void completedSprintAccountsForIncompleteSnapshot() {
        Sprint completedSprint = Sprint.builder()
                .id(4)
                .name("Completed sprint")
                .startDate(start)
                .endDate(end)
                .active(false)
                .isDone(true)
                .incompleteIssueCount(2)
                .incompleteEstimation(6)
                .build();
        Issue doneIssue = issue(DONE, 4, 3);
        doneIssue.setSprint(completedSprint);

        List<BurndownPoint> points = calculator.calculateBurndown(completedSprint, List.of(doneIssue));

        assertEquals(5, points.size());
        // Total scope = 4 (done) + 6 (incomplete snapshot) = 10 SP
        assertEquals(10, points.get(0).remainingWork()); // Nov 1
        assertEquals(10, points.get(1).remainingWork()); // Nov 2
        assertEquals(6, points.get(2).remainingWork());  // Nov 3 (done task completed)
        assertEquals(6, points.get(3).remainingWork());  // Nov 4
        assertEquals(6, points.get(4).remainingWork());  // Nov 5 (end of sprint, incomplete work left is 6)
    }

    @Test
    void sprintWithoutStartDateReturnsEmptyList() {
        Sprint unscheduled = Sprint.builder().id(5).build();
        assertEquals(List.of(), calculator.calculateBurndown(unscheduled, List.of()));
    }

    private Issue issue(IssueStatusID status, Integer estimation, Integer dayOfMonth) {
        Date modifiedDate = null;
        if (dayOfMonth != null) {
            modifiedDate = Date.from(ZonedDateTime.of(2026, 11, dayOfMonth, 12, 0, 0, 0, zone).toInstant());
        }
        Issue issue = Issue.builder()
                .issueStatus(new IssueStatus(status.getStatusID(), null))
                .estimation(estimation)
                .sprint(sprint)
                .build();
        issue.setLastModifiedDate(modifiedDate);
        return issue;
    }
}

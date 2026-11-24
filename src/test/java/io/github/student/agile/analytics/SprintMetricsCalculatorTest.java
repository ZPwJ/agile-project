package io.github.student.agile.analytics;

import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.IssueStatus;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.dictionary.IssueStatusID;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.CODE_REVIEW;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.DONE;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.IN_PROGRESS;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.IN_TESTS;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.TO_DO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SprintMetricsCalculatorTest {

    private final SprintMetricsCalculator calculator = new SprintMetricsCalculator();

    private final Sprint sprint = Sprint.builder().id(1).name("Sprint 1").build();
    private final Sprint otherSprint = Sprint.builder().id(2).name("Sprint 2").build();

    @Test
    void isCompletedOnlyForDoneStatus() {
        assertTrue(calculator.isCompleted(issue(DONE, 3)));
        for (IssueStatusID status : List.of(TO_DO, IN_PROGRESS, CODE_REVIEW, IN_TESTS)) {
            assertFalse(calculator.isCompleted(issue(status, 3)), status.name());
        }
    }

    @Test
    void issueWithoutStatusIsNotCompleted() {
        assertFalse(calculator.isCompleted(Issue.builder().estimation(5).sprint(sprint).build()));
    }

    @Test
    void estimationOrZeroReturnsEstimation() {
        assertEquals(8, calculator.estimationOrZero(issue(TO_DO, 8)));
        assertEquals(0, calculator.estimationOrZero(issue(TO_DO, 0)));
    }

    @Test
    void missingOrNegativeEstimationCountsAsZero() {
        assertEquals(0, calculator.estimationOrZero(issue(DONE, null)));
        assertEquals(0, calculator.estimationOrZero(issue(DONE, -4)));
    }

    @Test
    void belongsToMatchesSprintById() {
        Sprint sameSprintOtherInstance = Sprint.builder().id(1).build();
        Issue issue = issue(TO_DO, 1);
        issue.setSprint(sameSprintOtherInstance);

        assertTrue(calculator.belongsTo(issue, sprint));
        assertFalse(calculator.belongsTo(issue, otherSprint));
    }

    @Test
    void belongsToHandlesIssueWithoutSprintAndUnsavedSprint() {
        Sprint unsaved = Sprint.builder().name("not saved yet").build();
        Issue issueOfUnsaved = Issue.builder().sprint(unsaved).build();

        assertFalse(calculator.belongsTo(Issue.builder().build(), sprint));
        assertTrue(calculator.belongsTo(issueOfUnsaved, unsaved));
        assertFalse(calculator.belongsTo(issue(TO_DO, 1), unsaved));
    }

    @Test
    void calculateCountsIssuesAndSumsEstimationsByStatus() {
        List<Issue> issues = List.of(
                issue(DONE, 5),
                issue(DONE, 3),
                issue(IN_PROGRESS, 8),
                issue(TO_DO, 2));

        SprintMetrics metrics = calculator.calculate(sprint, issues);

        assertEquals(new SprintMetrics(4, 2, 2, 8, 10), metrics);
        assertEquals(18, metrics.totalEstimation());
    }

    @Test
    void calculateCountsIssuesWithoutEstimationButAddsNoPoints() {
        List<Issue> issues = List.of(issue(DONE, null), issue(TO_DO, null), issue(TO_DO, 5));

        SprintMetrics metrics = calculator.calculate(sprint, issues);

        assertEquals(new SprintMetrics(3, 1, 2, 0, 5), metrics);
    }

    @Test
    void calculateSkipsIssuesOfOtherSprintsAndNullEntries() {
        Issue foreign = issue(DONE, 13);
        foreign.setSprint(otherSprint);
        Issue backlogIssue = issue(DONE, 21);
        backlogIssue.setSprint(null);

        SprintMetrics metrics = calculator.calculate(sprint, Arrays.asList(issue(DONE, 2), foreign, backlogIssue, null));

        assertEquals(new SprintMetrics(1, 1, 0, 2, 0), metrics);
    }

    @Test
    void calculateReturnsZerosForEmptyOrMissingIssues() {
        assertSame(SprintMetrics.EMPTY, calculator.calculate(sprint, List.of()));
        assertSame(SprintMetrics.EMPTY, calculator.calculate(sprint, null));
        assertEquals(0, SprintMetrics.EMPTY.totalEstimation());
    }

    @Test
    void calculateRequiresSprint() {
        assertThrows(NullPointerException.class, () -> calculator.calculate(null, List.of()));
    }

    private Issue issue(IssueStatusID status, Integer estimation) {
        return Issue.builder()
                .issueStatus(new IssueStatus(status.getStatusID(), null))
                .estimation(estimation)
                .sprint(sprint)
                .build();
    }
}

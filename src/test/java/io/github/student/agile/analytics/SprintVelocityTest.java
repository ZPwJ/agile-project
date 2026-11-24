package io.github.student.agile.analytics;

import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.IssueStatus;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.dictionary.IssueStatusID;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.CODE_REVIEW;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.DONE;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.IN_PROGRESS;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.IN_TESTS;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.TO_DO;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * WF-2 acceptance cases: velocity is the sum of estimations of DONE issues of the sprint.
 */
class SprintVelocityTest {

    private final SprintMetricsCalculator calculator = new SprintMetricsCalculator();

    private final Sprint sprint = Sprint.builder().id(10).name("Sprint 10").build();

    @Test
    void sprintWithoutIssuesHasZeroVelocity() {
        assertEquals(0, calculator.velocity(sprint, List.of()));
        assertEquals(0, calculator.velocity(sprint, new HashSet<>()));
    }

    @Test
    void sprintWithOnlyNotCompletedIssuesHasZeroVelocity() {
        List<Issue> issues = List.of(
                issue(TO_DO, 5),
                issue(IN_PROGRESS, 3),
                issue(CODE_REVIEW, 8),
                issue(IN_TESTS, 2));

        assertEquals(0, calculator.velocity(sprint, issues));
    }

    @Test
    void doneIssueWithoutEstimationDoesNotFailAndAddsNothing() {
        List<Issue> issues = List.of(issue(DONE, null), issue(DONE, 5));

        assertEquals(5, calculator.velocity(sprint, issues));
    }

    @Test
    void onlyDoneIssuesAddToVelocity() {
        List<Issue> issues = List.of(
                issue(DONE, 5),
                issue(DONE, 8),
                issue(TO_DO, 13),
                issue(IN_PROGRESS, 3));

        assertEquals(13, calculator.velocity(sprint, issues));
    }

    @Test
    void doneIssuesOfOtherSprintDoNotAddToVelocity() {
        Issue otherSprintIssue = issue(DONE, 20);
        otherSprintIssue.setSprint(Sprint.builder().id(11).build());

        assertEquals(3, calculator.velocity(sprint, List.of(issue(DONE, 3), otherSprintIssue)));
    }

    @Test
    void velocityIsDeterministicForTheSameData() {
        List<Issue> issues = List.of(issue(DONE, 2), issue(DONE, 3), issue(TO_DO, 7));

        assertEquals(calculator.velocity(sprint, issues), calculator.velocity(sprint, issues));
        assertEquals(calculator.calculate(sprint, issues).velocity(), calculator.velocity(sprint, issues));
    }

    private Issue issue(IssueStatusID status, Integer estimation) {
        return Issue.builder()
                .issueStatus(new IssueStatus(status.getStatusID(), null))
                .estimation(estimation)
                .sprint(sprint)
                .build();
    }
}

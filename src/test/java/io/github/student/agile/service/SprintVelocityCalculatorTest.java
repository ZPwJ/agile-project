package io.github.student.agile.service;

import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.IssueStatus;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.dictionary.IssueStatusID;
import io.github.student.agile.resource.service.SprintVelocityCalculator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SprintVelocityCalculatorTest {

    private final SprintVelocityCalculator calculator = new SprintVelocityCalculator();

    @Test
    public void calculateVelocity_returnsZeroForEmptyAndNullInputs() {
        Sprint emptySprint = Sprint.builder()
                .id(1)
                .sprintIssues(Collections.emptySet())
                .build();
        Sprint nullIssuesSprint = Sprint.builder()
                .id(2)
                .sprintIssues(null)
                .build();

        assertEquals(0, calculator.calculateVelocity(emptySprint));
        assertEquals(0, calculator.calculateVelocity(nullIssuesSprint));
        assertEquals(0, calculator.calculateVelocity(null));
        assertEquals(0, calculator.calculateVelocity(1, Collections.emptyList()));
        assertEquals(0, calculator.calculateVelocity(1, null));
        assertEquals(0, calculator.calculateVelocity(null, List.of(createIssue(1, null, IssueStatusID.DONE, 5))));
    }

    @Test
    public void calculateVelocity_returnsZeroWhenSprintHasOnlyUncompletedIssues() {
        Sprint sprint = Sprint.builder().id(1).build();
        Set<Issue> issues = new HashSet<>(List.of(
                createIssue(1, sprint, IssueStatusID.TO_DO, 3),
                createIssue(2, sprint, IssueStatusID.IN_PROGRESS, 5),
                createIssue(3, sprint, IssueStatusID.CODE_REVIEW, 8),
                createIssue(4, sprint, IssueStatusID.IN_TESTS, 2),
                createIssue(5, sprint, null, 13)
        ));
        sprint.setSprintIssues(issues);

        assertEquals(0, calculator.calculateVelocity(sprint));
        assertEquals(0, calculator.calculateVelocity(sprint.getId(), issues));
    }

    @Test
    public void calculateVelocity_treatsNullEstimationAsZeroWithoutException() {
        Sprint sprint = Sprint.builder().id(1).build();
        Set<Issue> issues = new HashSet<>(List.of(
                createIssue(1, sprint, IssueStatusID.DONE, null),
                createIssue(2, sprint, IssueStatusID.DONE, null)
        ));
        sprint.setSprintIssues(issues);

        assertEquals(0, calculator.calculateVelocity(sprint));
        assertEquals(0, calculator.calculateVelocity(sprint.getId(), issues));
    }

    @Test
    public void calculateVelocity_sumsOnlyCompletedIssuesInMixedSprint() {
        Sprint sprint = Sprint.builder().id(1).build();
        Set<Issue> issues = new HashSet<>(List.of(
                createIssue(1, sprint, IssueStatusID.DONE, 3),
                createIssue(2, sprint, IssueStatusID.DONE, 5),
                createIssue(3, sprint, IssueStatusID.DONE, null),
                createIssue(4, sprint, IssueStatusID.IN_PROGRESS, 8),
                createIssue(5, sprint, IssueStatusID.CODE_REVIEW, 13),
                createIssue(6, sprint, IssueStatusID.TO_DO, 2)
        ));
        sprint.setSprintIssues(issues);

        assertEquals(8, calculator.calculateVelocity(sprint));
        assertEquals(8, calculator.calculateVelocity(sprint.getId(), issues));
    }

    @Test
    public void calculateVelocity_ignoresCompletedIssuesOutsideTargetSprintAndNullElements() {
        Sprint targetSprint = Sprint.builder().id(1).build();
        Sprint otherSprint = Sprint.builder().id(2).build();

        List<Issue> mixedIssues = Arrays.asList(
                createIssue(1, targetSprint, IssueStatusID.DONE, 5),
                createIssue(2, targetSprint, IssueStatusID.DONE, 3),
                createIssue(3, otherSprint, IssueStatusID.DONE, 13),
                createIssue(4, null, IssueStatusID.DONE, 21),
                null
        );
        targetSprint.setSprintIssues(new HashSet<>(mixedIssues));

        assertEquals(8, calculator.calculateVelocity(1, mixedIssues));
        assertEquals(8, calculator.calculateVelocity(targetSprint));
    }

    @Test
    public void calculateVelocity_supportsUnsavedSprintEntityWithMatchingReference() {
        Sprint unsavedSprint = Sprint.builder().build();
        Issue doneIssue = createIssue(1, unsavedSprint, IssueStatusID.DONE, 5);
        Issue uncompletedIssue = createIssue(2, unsavedSprint, IssueStatusID.IN_PROGRESS, 8);
        unsavedSprint.setSprintIssues(Set.of(doneIssue, uncompletedIssue));

        assertEquals(5, calculator.calculateVelocity(unsavedSprint));
    }

    @Test
    public void safeEstimation_handlesNullZeroPositiveAndNegativeValues() {
        assertEquals(0, calculator.safeEstimation((Issue) null));
        assertEquals(0, calculator.safeEstimation((Integer) null));
        assertEquals(0, calculator.safeEstimation(0));
        assertEquals(8, calculator.safeEstimation(8));
        assertEquals(0, calculator.safeEstimation(-5));
        assertEquals(5, calculator.safeEstimation(Issue.builder().estimation(5).build()));
        assertEquals(0, calculator.safeEstimation(Issue.builder().estimation(null).build()));
    }

    private Issue createIssue(Integer id, Sprint sprint, IssueStatusID statusId, Integer estimation) {
        IssueStatus status = statusId == null
                ? null
                : new IssueStatus(statusId.getStatusID(), null);
        return Issue.builder()
                .id(id)
                .sprint(sprint)
                .issueStatus(status)
                .estimation(estimation)
                .build();
    }
}

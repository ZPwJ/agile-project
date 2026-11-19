package io.github.student.agile.service.analytics;

import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.IssueStatus;
import io.github.student.agile.resource.domain.SprintIssueEvent;
import io.github.student.agile.resource.domain.dictionary.IssueStatusEnum;
import io.github.student.agile.resource.domain.dictionary.SprintIssueEventType;
import io.github.student.agile.resource.service.analytics.SprintIssueStates;
import io.github.student.agile.resource.service.analytics.SprintIssueStates.IssueState;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.DONE;
import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.IN_PROGRESS;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.ADDED;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.COMPLETED;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.ESTIMATION_CHANGED;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.REMOVED;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.SCOPE_AT_START;
import static io.github.student.agile.resource.service.analytics.SprintIssueStates.sumOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SprintIssueStatesTest {

    private static final Integer SPRINT_ID = 1;
    private static final ZonedDateTime START = ZonedDateTime.parse("2026-11-02T09:00:00+01:00");

    private int sequence = 0;

    @Test
    void replay_noEvents_returnsEmptyState() {

        SprintIssueStates states = SprintIssueStates.replay(List.of());

        assertEquals(0, states.getInitialIssueCount());
        assertTrue(states.completedIssues().isEmpty());
        assertTrue(states.uncompletedIssues().isEmpty());
    }

    @Test
    void replay_scopeAtStart_countsInitialIssuesAsUncompleted() {

        SprintIssueStates states = SprintIssueStates.replay(List.of(
                event(1, SCOPE_AT_START, 5),
                event(2, SCOPE_AT_START, 3)));

        assertEquals(2, states.getInitialIssueCount());
        assertEquals(List.of(1, 2), ids(states.uncompletedIssues()));
        assertTrue(states.completedIssues().isEmpty());
    }

    @Test
    void replay_issueAddedAfterStart_isInScopeButNotInitial() {

        SprintIssueStates states = SprintIssueStates.replay(List.of(
                event(1, SCOPE_AT_START, 5),
                event(2, ADDED, 8)));

        assertEquals(1, states.getInitialIssueCount());
        assertEquals(List.of(1, 2), ids(states.uncompletedIssues()));
    }

    @Test
    void replay_completedIssue_usesEstimationAtCompletion() {

        SprintIssueStates states = SprintIssueStates.replay(List.of(
                event(1, SCOPE_AT_START, 5),
                event(1, ESTIMATION_CHANGED, 8),
                event(1, COMPLETED, 8),
                event(1, ESTIMATION_CHANGED, 13)));

        List<IssueState> completed = states.completedIssues();
        assertEquals(1, completed.size());
        assertEquals(8, completed.get(0).getCompletedEstimation());
        assertEquals(13, completed.get(0).getEstimation());
    }

    @Test
    void replay_uncompletedIssue_usesLatestEstimation() {

        SprintIssueStates states = SprintIssueStates.replay(List.of(
                event(1, SCOPE_AT_START, 5),
                event(1, ESTIMATION_CHANGED, 2)));

        assertEquals(2, states.uncompletedIssues().get(0).getEstimation());
    }

    @Test
    void replay_removedIssue_isIgnoredEvenIfCompletedBefore() {

        SprintIssueStates states = SprintIssueStates.replay(List.of(
                event(1, SCOPE_AT_START, 5),
                event(2, SCOPE_AT_START, 3),
                event(1, COMPLETED, 5),
                event(1, REMOVED, 5),
                event(2, REMOVED, 3)));

        assertEquals(2, states.getInitialIssueCount());
        assertTrue(states.completedIssues().isEmpty());
        assertTrue(states.uncompletedIssues().isEmpty());
    }

    @Test
    void replay_issueRemovedAndAddedAgain_isInScope() {

        SprintIssueStates states = SprintIssueStates.replay(List.of(
                event(1, SCOPE_AT_START, 5),
                event(1, REMOVED, 5),
                event(1, ADDED, 3)));

        assertEquals(1, states.getInitialIssueCount());
        assertEquals(3, states.uncompletedIssues().get(0).getEstimation());
    }

    @Test
    void replay_issueWithoutEstimation_isCountedWithNullEstimation() {

        SprintIssueStates states = SprintIssueStates.replay(List.of(
                event(1, SCOPE_AT_START, null),
                event(1, COMPLETED, null)));

        List<IssueState> completed = states.completedIssues();
        assertEquals(1, completed.size());
        assertEquals(0, sumOf(completed.stream().map(IssueState::getCompletedEstimation).toList()));
    }

    @Test
    void fromCurrentIssues_doneStatusMeansCompleted() {

        Issue done = issue(1, 5, DONE.getStatusID());
        Issue inProgress = issue(2, 3, IN_PROGRESS.getStatusID());
        Issue withoutStatus = issue(3, null, null);

        SprintIssueStates states = SprintIssueStates.fromCurrentIssues(List.of(done, inProgress, withoutStatus));

        assertEquals(3, states.getInitialIssueCount());
        assertEquals(List.of(1), ids(states.completedIssues()));
        assertEquals(5, states.completedIssues().get(0).getCompletedEstimation());
        assertEquals(List.of(2, 3), ids(states.uncompletedIssues()));
    }

    @Test
    void sumOf_treatsMissingEstimationAsZero() {

        assertEquals(0, sumOf(List.of()));
        assertEquals(8, sumOf(Arrays.asList(5, null, 3)));
        assertEquals(0, sumOf(new ArrayList<>(Arrays.asList(null, null))));
    }

    private SprintIssueEvent event(Integer issueID, SprintIssueEventType type, Integer estimation) {
        sequence++;
        return new SprintIssueEvent(sequence, SPRINT_ID, issueID, type, estimation, START.plusMinutes(sequence));
    }

    private static Issue issue(Integer id, Integer estimation, Integer statusID) {
        IssueStatus status = statusID == null ? null : new IssueStatus(statusID, IssueStatusEnum.values()[statusID - 1]);
        return Issue.builder().id(id).estimation(estimation).issueStatus(status).build();
    }

    private static List<Integer> ids(List<IssueState> states) {
        return states.stream().map(IssueState::getIssueID).toList();
    }
}

package io.github.student.agile.resource.service.analytics;

import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.SprintIssueEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.DONE;
import static io.github.student.agile.resource.domain.dictionary.SprintIssueEventType.SCOPE_AT_START;

/**
 * Final state of every issue that has ever belonged to a sprint.
 * <p>
 * For a started sprint the state is rebuilt by replaying its events in chronological order, because
 * finishing a sprint detaches its issues, so the current {@link Issue} rows no longer describe it.
 * An issue counts for the sprint only if it is still in scope after the last event: an issue removed
 * from the sprint (or deleted) is ignored, even if it was completed before the removal.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class SprintIssueStates {

    @Getter
    private final int initialIssueCount;
    private final Collection<IssueState> states;

    public static SprintIssueStates replay(List<SprintIssueEvent> chronologicalEvents) {
        Map<Integer, IssueState> states = new LinkedHashMap<>();
        int initialIssueCount = 0;

        for (SprintIssueEvent event : chronologicalEvents) {
            IssueState state = states.computeIfAbsent(event.getIssueID(), IssueState::new);
            switch (event.getType()) {
                case SCOPE_AT_START, ADDED -> {
                    state.inScope = true;
                    state.estimation = event.getEstimation();
                }
                case REMOVED -> state.inScope = false;
                case ESTIMATION_CHANGED -> state.estimation = event.getEstimation();
                case COMPLETED -> {
                    state.completed = true;
                    state.estimation = event.getEstimation();
                    state.completedEstimation = event.getEstimation();
                }
            }
            if (event.getType() == SCOPE_AT_START) {
                initialIssueCount++;
            }
        }
        return new SprintIssueStates(initialIssueCount, states.values());
    }

    /**
     * Fallback for a sprint without recorded history (not started yet, or started before events were recorded):
     * the state is taken from the issues currently assigned to the sprint.
     */
    public static SprintIssueStates fromCurrentIssues(Collection<Issue> issues) {
        List<IssueState> states = new ArrayList<>();
        for (Issue issue : issues) {
            IssueState state = new IssueState(issue.getId());
            state.inScope = true;
            state.estimation = issue.getEstimation();
            state.completed = issue.getIssueStatus() != null && DONE.getStatusID().equals(issue.getIssueStatus().getId());
            state.completedEstimation = state.completed ? issue.getEstimation() : null;
            states.add(state);
        }
        return new SprintIssueStates(states.size(), states);
    }

    public List<IssueState> completedIssues() {
        return select(state -> state.inScope && state.completed);
    }

    public List<IssueState> uncompletedIssues() {
        return select(state -> state.inScope && !state.completed);
    }

    private List<IssueState> select(Predicate<IssueState> predicate) {
        return states.stream().filter(predicate).toList();
    }

    /** Sums estimations, treating a missing estimation as 0 points. */
    public static int sumOf(Collection<Integer> estimations) {
        return estimations.stream()
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class IssueState {

        private final Integer issueID;
        private boolean inScope;
        private boolean completed;
        /** Latest known estimation, may be null. */
        private Integer estimation;
        /** Estimation at the moment of completion, may be null. */
        private Integer completedEstimation;
    }
}

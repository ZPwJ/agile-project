package io.github.student.agile.resource.service.analytics;

import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintIssueEvent;
import io.github.student.agile.resource.service.analytics.SprintIssueStates.IssueState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

import static io.github.student.agile.resource.service.analytics.SprintIssueStates.sumOf;
import static java.util.Objects.isNull;

/**
 * Single place where sprint metrics, including velocity (WF-2), are calculated.
 * <p>
 * An issue is completed when it reached status {@code DONE}, the only terminal status of the issue flow.
 * Velocity is the sum of estimations of issues that are still assigned to the sprint and completed in it;
 * each issue contributes its estimation from the moment of completion, so later estimation changes do not
 * alter the velocity of a sprint. An issue without estimation contributes 0 points.
 * <p>
 * The calculator does not query the database: callers pass the sprint's events, which lets a report covering
 * many sprints load all events with one query.
 */
@Component
public class SprintMetricsCalculator {

    /** Events exist only for sprints started after the analytics module was introduced (startedAt is set on start). */
    public boolean hasHistory(Sprint sprint) {
        return !isNull(sprint.getStartedAt());
    }

    /**
     * @param chronologicalEvents events of this sprint only, ordered by occurrence; ignored when the sprint has no history
     */
    public SprintMetrics calculate(Sprint sprint, List<SprintIssueEvent> chronologicalEvents) {

        boolean historyAvailable = hasHistory(sprint);
        SprintIssueStates states = historyAvailable
                ? SprintIssueStates.replay(chronologicalEvents)
                : SprintIssueStates.fromCurrentIssues(isNull(sprint.getSprintIssues()) ? Set.of() : sprint.getSprintIssues());

        List<IssueState> uncompleted = states.uncompletedIssues();
        return new SprintMetrics(
                historyAvailable,
                states.getInitialIssueCount(),
                states.completedIssues().size(),
                uncompleted.size(),
                velocity(states),
                sumOf(uncompleted.stream().map(IssueState::getEstimation).toList()));
    }

    public int velocity(SprintIssueStates states) {
        return sumOf(states.completedIssues().stream().map(IssueState::getCompletedEstimation).toList());
    }
}

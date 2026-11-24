package io.github.student.agile.analytics;

import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.IssueStatus;
import io.github.student.agile.resource.domain.Sprint;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;

import static io.github.student.agile.resource.domain.dictionary.IssueStatusID.DONE;

/**
 * Shared rules for sprint metrics. The sprint report (WF-1), burndown (WF-3), burnup (WF-4)
 * and sprint comparison (WF-5, WF-7) use these methods instead of computing metrics on their own.
 *
 * <p>Rules:
 * <ul>
 *     <li>an issue is completed when its status is {@code DONE}; an issue without status is not completed,</li>
 *     <li>a missing or negative estimation counts as 0 story points,</li>
 *     <li>only issues assigned to the given sprint are counted, any other issue is skipped.</li>
 * </ul>
 */
@Component
public class SprintMetricsCalculator {

    public boolean isCompleted(Issue issue) {
        IssueStatus status = issue.getIssueStatus();
        return status != null && DONE.getStatusID().equals(status.getId());
    }

    public int estimationOrZero(Issue issue) {
        Integer estimation = issue.getEstimation();
        return estimation == null || estimation < 0 ? 0 : estimation;
    }

    public boolean belongsTo(Issue issue, Sprint sprint) {
        Sprint issueSprint = issue.getSprint();
        if (issueSprint == null) {
            return false;
        }
        if (issueSprint == sprint) {
            return true;
        }
        return sprint.getId() != null && sprint.getId().equals(issueSprint.getId());
    }

    /**
     * Velocity of the sprint (WF-2): sum of estimations of DONE issues assigned to it.
     * Not completed issues, issues of other sprints and issues without estimation add nothing.
     * The current estimation of each issue is used, the application does not keep estimation history.
     */
    public int velocity(Sprint sprint, Collection<Issue> issues) {
        return calculate(sprint, issues).velocity();
    }

    /**
     * Computes sprint metrics in a single pass over the issues.
     *
     * @param sprint sprint to compute metrics for
     * @param issues issues loaded for the sprint; issues of other sprints are ignored, {@code null} means no issues
     */
    public SprintMetrics calculate(Sprint sprint, Collection<Issue> issues) {
        Objects.requireNonNull(sprint, "sprint must not be null");
        if (issues == null || issues.isEmpty()) {
            return SprintMetrics.EMPTY;
        }

        int completed = 0;
        int incomplete = 0;
        int completedEstimation = 0;
        int incompleteEstimation = 0;

        for (Issue issue : issues) {
            if (issue == null || !belongsTo(issue, sprint)) {
                continue;
            }
            if (isCompleted(issue)) {
                completed++;
                completedEstimation += estimationOrZero(issue);
            } else {
                incomplete++;
                incompleteEstimation += estimationOrZero(issue);
            }
        }

        return new SprintMetrics(completed + incomplete, completed, incomplete, completedEstimation, incompleteEstimation);
    }
}

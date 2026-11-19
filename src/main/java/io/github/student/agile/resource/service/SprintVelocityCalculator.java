package io.github.student.agile.resource.service;

import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.dictionary.IssueStatusID;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class SprintVelocityCalculator {

    public int calculateVelocity(Sprint sprint) {
        if (sprint == null || sprint.getSprintIssues() == null) {
            return 0;
        }
        if (sprint.getId() != null) {
            return calculateVelocity(sprint.getId(), sprint.getSprintIssues());
        }
        return sprint.getSprintIssues().stream()
                .filter(issue -> issue != null && issue.getSprint() == sprint)
                .filter(IssueStatusID::isCompleted)
                .mapToInt(this::safeEstimation)
                .sum();
    }

    public int calculateVelocity(Integer sprintId, Collection<Issue> issues) {
        if (sprintId == null || issues == null || issues.isEmpty()) {
            return 0;
        }
        return issues.stream()
                .filter(issue -> belongsToSprint(issue, sprintId))
                .filter(IssueStatusID::isCompleted)
                .mapToInt(this::safeEstimation)
                .sum();
    }

    public int safeEstimation(Issue issue) {
        return issue == null ? 0 : safeEstimation(issue.getEstimation());
    }

    public int safeEstimation(Integer estimation) {
        return estimation == null ? 0 : Math.max(0, estimation);
    }

    private boolean belongsToSprint(Issue issue, Integer sprintId) {
        return issue != null
                && issue.getSprint() != null
                && sprintId.equals(issue.getSprint().getId());
    }
}

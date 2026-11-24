package io.github.student.agile.analytics;

import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SprintAnalyticsService {

    private final SprintRepository sprintRepository;
    private final IssueRepository issueRepository;
    private final SprintMetricsCalculator metricsCalculator;

    @Transactional(readOnly = true)
    public SprintReport getSprintReport(Integer sprintId) {
        if (sprintId == null || sprintId <= 0) {
            throw new IllegalArgumentException("Sprint ID must be a positive integer");
        }
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new AnalyticsNotFoundException("Sprint not found: " + sprintId));
        SprintMetrics metrics = metricsCalculator.calculate(sprint, issueRepository.findAllBySprint(sprint));

        int incompleteIssues = metrics.incompleteIssues();
        int incompleteEstimation = metrics.incompleteEstimation();
        if (Boolean.TRUE.equals(sprint.getIsDone())) {
            if (sprint.getIncompleteIssueCount() == null || sprint.getIncompleteEstimation() == null) {
                throw new IllegalStateException("Completed sprint has no incomplete issue snapshot: " + sprintId);
            }
            incompleteIssues += sprint.getIncompleteIssueCount();
            incompleteEstimation += sprint.getIncompleteEstimation();
        }
        return new SprintReport(sprint.getId(), sprint.getName(), sprint.getProject().getId(),
                sprint.getStartDate(), sprint.getEndDate(), metrics.completedIssues() + incompleteIssues,
                metrics.completedIssues(), incompleteIssues, metrics.velocity(), incompleteEstimation);
    }

    @Transactional(readOnly = true)
    public List<BurndownPoint> getSprintBurndown(Integer sprintId) {
        if (sprintId == null || sprintId <= 0) {
            throw new IllegalArgumentException("Sprint ID must be a positive integer");
        }
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new AnalyticsNotFoundException("Sprint not found: " + sprintId));

        if (Boolean.TRUE.equals(sprint.getIsDone())) {
            if (sprint.getIncompleteIssueCount() == null || sprint.getIncompleteEstimation() == null) {
                throw new IllegalStateException("Completed sprint has no incomplete issue snapshot: " + sprintId);
            }
        }

        return metricsCalculator.calculateBurndown(sprint, issueRepository.findAllBySprint(sprint));
    }
}

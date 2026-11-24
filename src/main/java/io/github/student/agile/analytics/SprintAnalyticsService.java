package io.github.student.agile.analytics;

import io.github.student.agile.resource.domain.Issue;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.repository.IssueRepository;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;

@Service
@RequiredArgsConstructor
public class SprintAnalyticsService {

    private static final String STORY_POINTS = "STORY_POINTS";

    private final SprintRepository sprintRepository;
    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final SprintMetricsCalculator metricsCalculator;

    @Transactional(readOnly = true)
    public SprintReport getSprintReport(Integer sprintId) {
        if (sprintId == null || sprintId <= 0) {
            throw new IllegalArgumentException("Sprint ID must be a positive integer");
        }
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new AnalyticsNotFoundException("Sprint not found: " + sprintId));
        SprintMetrics metrics = metricsCalculator.calculate(sprint, issueRepository.findAllBySprint(sprint));

        int incompleteIssues = incompleteIssueCount(sprint, metrics);
        int incompleteEstimation = metrics.incompleteEstimation();
        if (Boolean.TRUE.equals(sprint.getIsDone())) {
            if (sprint.getIncompleteEstimation() == null) {
                throw new IllegalStateException("Completed sprint has no incomplete issue snapshot: " + sprintId);
            }
            incompleteEstimation += sprint.getIncompleteEstimation();
        }
        return new SprintReport(sprint.getId(), sprint.getName(), sprint.getProject().getId(),
                sprint.getStartDate(), sprint.getEndDate(), metrics.completedIssues() + incompleteIssues,
                metrics.completedIssues(), incompleteIssues, metrics.velocity(), incompleteEstimation);
    }

    @Transactional(readOnly = true)
    public List<SprintComparisonDTO> compareSprints(Integer projectId, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Parameter 'from' must not be after 'to'");
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AnalyticsNotFoundException("Project not found: " + projectId));

        List<Sprint> sprints = sprintRepository.findAllByProject(project).stream()
                .filter(sprint -> matchesDateFilter(sprint, from, to))
                .sorted(Comparator.comparing(Sprint::getStartDate, nullsLast(naturalOrder()))
                        .thenComparing(Sprint::getId, nullsLast(naturalOrder())))
                .toList();

        Map<Integer, List<Issue>> issuesBySprintId = loadIssuesBySprint(sprints);

        List<SprintComparisonDTO> result = new ArrayList<>(sprints.size());
        for (Sprint sprint : sprints) {
            List<Issue> issues = issuesBySprintId.getOrDefault(sprint.getId(), List.of());
            SprintMetrics metrics = metricsCalculator.calculate(sprint, issues);
            result.add(new SprintComparisonDTO(
                    project.getId(),
                    sprint.getId(),
                    sprint.getName(),
                    sprint.getStartDate(),
                    sprint.getEndDate(),
                    resolveStatus(sprint),
                    metrics.completedIssues(),
                    incompleteIssueCount(sprint, metrics),
                    metrics.velocity(),
                    STORY_POINTS));
        }
        return result;
    }

    private Map<Integer, List<Issue>> loadIssuesBySprint(List<Sprint> sprints) {
        if (sprints.isEmpty()) {
            return Map.of();
        }
        return issueRepository.findAllBySprintIn(sprints).stream()
                .filter(issue -> issue.getSprint() != null && issue.getSprint().getId() != null)
                .collect(Collectors.groupingBy(issue -> issue.getSprint().getId()));
    }

    private static boolean matchesDateFilter(Sprint sprint, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return true;
        }
        if (sprint.getStartDate() == null) {
            return false;
        }
        LocalDate startDate = sprint.getStartDate().toLocalDate();
        if (from != null && startDate.isBefore(from)) {
            return false;
        }
        if (to != null && startDate.isAfter(to)) {
            return false;
        }
        return true;
    }

    private int incompleteIssueCount(Sprint sprint, SprintMetrics metrics) {
        int incompleteIssues = metrics.incompleteIssues();
        if (Boolean.TRUE.equals(sprint.getIsDone())) {
            if (sprint.getIncompleteIssueCount() == null) {
                throw new IllegalStateException("Completed sprint has no incomplete issue snapshot: " + sprint.getId());
            }
            incompleteIssues += sprint.getIncompleteIssueCount();
        }
        return incompleteIssues;
    }

    static SprintComparisonStatus resolveStatus(Sprint sprint) {
        if (Boolean.TRUE.equals(sprint.getIsDone())) {
            return SprintComparisonStatus.DONE;
        }
        if (Boolean.TRUE.equals(sprint.getActive())) {
            return SprintComparisonStatus.ACTIVE;
        }
        return SprintComparisonStatus.PLANNED;
    }
}
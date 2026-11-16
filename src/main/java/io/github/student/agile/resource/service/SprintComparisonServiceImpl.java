package io.github.student.agile.resource.service;

import io.github.student.agile.exception.definition.ResourceNotFoundException;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintIssueEvent;
import io.github.student.agile.resource.dto.analytics.SprintComparisonDTO;
import io.github.student.agile.resource.dto.analytics.SprintComparisonDTO.SprintResult;
import io.github.student.agile.resource.dto.analytics.SprintReportDTO;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintIssueEventRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.analytics.SprintMetrics;
import io.github.student.agile.resource.service.analytics.SprintMetricsCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.PROJECT_DOES_NOT_EXISTS;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static java.util.stream.Collectors.groupingBy;

/**
 * Compares the finished sprints of a project (WF-5). Metrics of every sprint come from {@link SprintMetricsCalculator},
 * so they are equal to the ones in the sprint report.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SprintComparisonServiceImpl implements SprintComparisonService {

    /**
     * From the oldest to the newest. A finished sprint without {@code startedAt} was started before the analytics
     * module recorded history, so it is older than every sprint with a recorded start.
     */
    static final Comparator<Sprint> CHRONOLOGICAL_ORDER = Comparator
            .comparing(Sprint::getStartedAt, nullsFirst(naturalOrder()))
            .thenComparing(Sprint::getId);

    private final ProjectRepository projectRepository;
    private final SprintRepository sprintRepository;
    private final SprintIssueEventRepository sprintIssueEventRepository;
    private final SprintMetricsCalculator sprintMetricsCalculator;

    @Override
    public SprintComparisonDTO compareSprints(Integer projectID) {

        if (!projectRepository.existsById(projectID)) {
            throw new ResourceNotFoundException(PROJECT_DOES_NOT_EXISTS);
        }

        List<Sprint> sprints = sprintRepository.findAllByProjectIdAndIsDoneTrue(projectID)
                .stream()
                .sorted(CHRONOLOGICAL_ORDER)
                .toList();
        Map<Integer, List<SprintIssueEvent>> eventsBySprint = loadEvents(sprints);

        return SprintComparisonDTO.builder()
                .projectID(projectID)
                .estimationUnit(SprintReportDTO.STORY_POINTS)
                .sprints(sprints.stream()
                        .map(sprint -> toResult(sprint, eventsBySprint.getOrDefault(sprint.getId(), List.of())))
                        .toList())
                .build();
    }

    /** Loads events of all sprints with one query (WNF-5); grouping keeps the chronological order within each sprint. */
    private Map<Integer, List<SprintIssueEvent>> loadEvents(List<Sprint> sprints) {

        List<Integer> sprintIDs = sprints.stream()
                .filter(sprintMetricsCalculator::hasHistory)
                .map(Sprint::getId)
                .toList();
        if (sprintIDs.isEmpty()) {
            return Map.of();
        }
        return sprintIssueEventRepository.findAllBySprintIDInOrderByOccurredAtAscIdAsc(sprintIDs)
                .stream()
                .collect(groupingBy(SprintIssueEvent::getSprintID));
    }

    private SprintResult toResult(Sprint sprint, List<SprintIssueEvent> events) {

        SprintMetrics metrics = sprintMetricsCalculator.calculate(sprint, events);
        return new SprintResult(
                sprint.getId(),
                sprint.getName(),
                sprint.getStartedAt(),
                sprint.getEndedAt(),
                metrics.historyAvailable(),
                metrics.completedIssueCount(),
                metrics.uncompletedIssueCount(),
                metrics.velocity());
    }
}

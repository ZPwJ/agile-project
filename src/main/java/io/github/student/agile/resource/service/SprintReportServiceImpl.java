package io.github.student.agile.resource.service;

import io.github.student.agile.exception.definition.ResourceNotFoundException;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintIssueEvent;
import io.github.student.agile.resource.dto.analytics.SprintReportDTO;
import io.github.student.agile.resource.dto.analytics.SprintState;
import io.github.student.agile.resource.repository.SprintIssueEventRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.analytics.SprintMetrics;
import io.github.student.agile.resource.service.analytics.SprintMetricsCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_DOES_NOT_EXISTS;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SprintReportServiceImpl implements SprintReportService {

    private final SprintRepository sprintRepository;
    private final SprintIssueEventRepository sprintIssueEventRepository;
    private final SprintMetricsCalculator sprintMetricsCalculator;

    @Override
    public SprintReportDTO getSprintReport(Integer sprintID) {

        Sprint sprint = sprintRepository.findById(sprintID)
                .orElseThrow(() -> new ResourceNotFoundException(SPRINT_DOES_NOT_EXISTS));

        List<SprintIssueEvent> events = sprintMetricsCalculator.hasHistory(sprint)
                ? sprintIssueEventRepository.findAllBySprintIDOrderByOccurredAtAscIdAsc(sprintID)
                : List.of();
        SprintMetrics metrics = sprintMetricsCalculator.calculate(sprint, events);

        return SprintReportDTO.builder()
                .sprintID(sprint.getId())
                .sprintName(sprint.getName())
                .projectID(sprint.getProject().getId())
                .state(SprintState.of(sprint))
                .plannedStartDate(sprint.getStartDate())
                .startedAt(sprint.getStartedAt())
                .endedAt(sprint.getEndedAt())
                .historyAvailable(metrics.historyAvailable())
                .initialIssueCount(metrics.initialIssueCount())
                .completedIssueCount(metrics.completedIssueCount())
                .uncompletedIssueCount(metrics.uncompletedIssueCount())
                .velocity(metrics.velocity())
                .uncompletedEstimation(metrics.uncompletedEstimation())
                .estimationUnit(SprintReportDTO.STORY_POINTS)
                .build();
    }
}

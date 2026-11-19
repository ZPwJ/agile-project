package io.github.student.agile.resource.analytics.metrics;

import io.github.student.agile.resource.analytics.error.SprintNotFoundException;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.dto.SprintSummaryDTO;
import io.github.student.agile.resource.dto.VelocityDTO;
import io.github.student.agile.resource.repository.SprintRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Builds the WF-1 and WF-2 responses from {@link SprintMetricsProvider}; contains no counting logic. */
@Service
@Transactional
@RequiredArgsConstructor
public class SprintReportService {

    private final SprintRepository sprintRepository;
    private final SprintMetricsProvider metricsProvider;

    public SprintSummaryDTO getSummary(Integer sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new SprintNotFoundException(sprintId));
        SprintMetrics metrics = metricsProvider.forSprint(sprintId);

        return SprintSummaryDTO.builder()
                .sprintId(sprint.getId())
                .sprintName(sprint.getName())
                .projectId(sprint.getProject().getId())
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .state(metrics.state())
                .unit(metrics.unit())
                .totalTasks(metrics.totalTasks())
                .completedTasks(metrics.completedTasks())
                .incompleteTasks(metrics.incompleteTasks())
                .unestimatedTasks(metrics.unestimatedTasks())
                .velocity(metrics.completedEstimate())
                .incompleteEstimate(metrics.incompleteEstimate())
                .build();
    }

    public VelocityDTO getVelocity(Integer sprintId) {
        SprintMetrics metrics = metricsProvider.forSprint(sprintId);
        return VelocityDTO.builder()
                .sprintId(metrics.sprintId())
                .state(metrics.state())
                .unit(metrics.unit())
                .velocity(metrics.completedEstimate())
                .build();
    }
}

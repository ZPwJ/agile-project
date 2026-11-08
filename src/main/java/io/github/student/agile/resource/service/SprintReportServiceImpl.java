package io.github.student.agile.resource.service;

import io.github.student.agile.exception.definition.ResourceNotFoundException;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.dto.analytics.SprintReportDTO;
import io.github.student.agile.resource.dto.analytics.SprintState;
import io.github.student.agile.resource.repository.SprintIssueEventRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.analytics.SprintIssueStates;
import io.github.student.agile.resource.service.analytics.SprintIssueStates.IssueState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_DOES_NOT_EXISTS;
import static io.github.student.agile.resource.service.analytics.SprintIssueStates.sumOf;
import static java.util.Objects.isNull;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SprintReportServiceImpl implements SprintReportService {

    private final SprintRepository sprintRepository;
    private final SprintIssueEventRepository sprintIssueEventRepository;

    @Override
    public SprintReportDTO getSprintReport(Integer sprintID) {

        Sprint sprint = sprintRepository.findById(sprintID)
                .orElseThrow(() -> new ResourceNotFoundException(SPRINT_DOES_NOT_EXISTS));

        // Events exist only for sprints started after the analytics module was introduced (startedAt is set on start).
        boolean historyAvailable = !isNull(sprint.getStartedAt());
        SprintIssueStates states = historyAvailable
                ? SprintIssueStates.replay(sprintIssueEventRepository.findAllBySprintIDOrderByOccurredAtAscIdAsc(sprintID))
                : SprintIssueStates.fromCurrentIssues(isNull(sprint.getSprintIssues()) ? Set.of() : sprint.getSprintIssues());

        List<IssueState> completed = states.completedIssues();
        List<IssueState> uncompleted = states.uncompletedIssues();

        return SprintReportDTO.builder()
                .sprintID(sprint.getId())
                .sprintName(sprint.getName())
                .projectID(sprint.getProject().getId())
                .state(SprintState.of(sprint))
                .plannedStartDate(sprint.getStartDate())
                .startedAt(sprint.getStartedAt())
                .endedAt(sprint.getEndedAt())
                .historyAvailable(historyAvailable)
                .initialIssueCount(states.getInitialIssueCount())
                .completedIssueCount(completed.size())
                .uncompletedIssueCount(uncompleted.size())
                .velocity(sumOf(completed.stream().map(IssueState::getCompletedEstimation).toList()))
                .uncompletedEstimation(sumOf(uncompleted.stream().map(IssueState::getEstimation).toList()))
                .estimationUnit(SprintReportDTO.STORY_POINTS)
                .build();
    }
}

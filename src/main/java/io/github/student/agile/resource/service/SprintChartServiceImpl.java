package io.github.student.agile.resource.service;

import io.github.student.agile.exception.definition.ResourceNotFoundException;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintIssueEvent;
import io.github.student.agile.resource.dto.analytics.SprintBurndownDTO;
import io.github.student.agile.resource.dto.analytics.SprintBurnupDTO;
import io.github.student.agile.resource.dto.analytics.SprintReportDTO;
import io.github.student.agile.resource.dto.analytics.SprintState;
import io.github.student.agile.resource.repository.SprintIssueEventRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.analytics.SprintMetricsCalculator;
import io.github.student.agile.resource.service.analytics.SprintProgress;
import io.github.student.agile.resource.service.analytics.SprintProgressCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_DOES_NOT_EXISTS;
import static io.github.student.agile.resource.service.analytics.SprintProgressCalculator.REPORT_ZONE;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SprintChartServiceImpl implements SprintChartService {

    private final SprintRepository sprintRepository;
    private final SprintIssueEventRepository sprintIssueEventRepository;
    private final SprintMetricsCalculator sprintMetricsCalculator;
    private final SprintProgressCalculator sprintProgressCalculator;

    @Override
    public SprintBurndownDTO getBurndown(Integer sprintID) {

        Sprint sprint = findSprint(sprintID);
        SprintProgress progress = progressOf(sprint);

        return SprintBurndownDTO.builder()
                .sprintID(sprint.getId())
                .sprintName(sprint.getName())
                .projectID(sprint.getProject().getId())
                .state(SprintState.of(sprint))
                .startedAt(sprint.getStartedAt())
                .endedAt(sprint.getEndedAt())
                .historyAvailable(progress.historyAvailable())
                .initialScope(progress.initialScope())
                .estimationUnit(SprintReportDTO.STORY_POINTS)
                .timeZone(REPORT_ZONE.getId())
                .points(progress.days().stream()
                        .map(day -> new SprintBurndownDTO.Point(day.day(), day.date(),
                                Math.max(0, progress.initialScope() - day.completedWork()),
                                Math.max(0, day.totalScope() - day.completedWork())))
                        .toList())
                .build();
    }

    @Override
    public SprintBurnupDTO getBurnup(Integer sprintID) {

        Sprint sprint = findSprint(sprintID);
        SprintProgress progress = progressOf(sprint);

        return SprintBurnupDTO.builder()
                .sprintID(sprint.getId())
                .sprintName(sprint.getName())
                .projectID(sprint.getProject().getId())
                .state(SprintState.of(sprint))
                .startedAt(sprint.getStartedAt())
                .endedAt(sprint.getEndedAt())
                .historyAvailable(progress.historyAvailable())
                .estimationUnit(SprintReportDTO.STORY_POINTS)
                .timeZone(REPORT_ZONE.getId())
                .points(progress.days().stream()
                        .map(day -> new SprintBurnupDTO.Point(day.day(), day.date(), day.completedWork(), day.totalScope()))
                        .toList())
                .build();
    }

    private Sprint findSprint(Integer sprintID) {
        return sprintRepository.findById(sprintID)
                .orElseThrow(() -> new ResourceNotFoundException(SPRINT_DOES_NOT_EXISTS));
    }

    private SprintProgress progressOf(Sprint sprint) {
        List<SprintIssueEvent> events = sprintMetricsCalculator.hasHistory(sprint)
                ? sprintIssueEventRepository.findAllBySprintIDOrderByOccurredAtAscIdAsc(sprint.getId())
                : List.of();
        return sprintProgressCalculator.calculate(sprint, events, ZonedDateTime.now(REPORT_ZONE));
    }
}

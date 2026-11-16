package io.github.student.agile.resource.service;

import io.github.student.agile.exception.definition.ResourceNotFoundException;
import io.github.student.agile.exception.definition.StateConflictException;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintProgress;
import io.github.student.agile.resource.dto.BurndownPointDTO;
import io.github.student.agile.resource.dto.BurnupPointDTO;
import io.github.student.agile.resource.dto.SprintReportDTO;
import io.github.student.agile.resource.repository.SprintProgressRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_DOES_NOT_EXISTS;
import static io.github.student.agile.resource.service.RequestValidation.requirePositiveId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SprintReportService {

    private final SprintRepository sprintRepository;
    private final SprintProgressRepository progressRepository;

    public SprintReportDTO getReport(Integer sprintID) {
        Sprint sprint = requireSprint(sprintID);
        if (!Boolean.TRUE.equals(sprint.getIsDone())) {
            throw new StateConflictException("Sprint report requires a closed sprint");
        }

        List<SprintProgress> history = progressRepository.findAllBySprintIdOrderByCapturedAtAscIdAsc(sprintID);
        SprintProgress result = history.stream()
                .filter(point -> point.getKind() == SprintProgress.Kind.END)
                .reduce((earlier, later) -> later)
                .orElseThrow(() -> new StateConflictException("Sprint has no recorded final result"));
        Integer initialCount = history.stream()
                .filter(point -> point.getKind() == SprintProgress.Kind.START)
                .findFirst().map(SprintProgress::getIssueCount).orElse(null);

        return new SprintReportDTO(sprint.getId(), sprint.getName(), sprint.getProject().getId(),
                instant(sprint.getStartDate()), instant(sprint.getEndDate()), initialCount,
                result.getCompletedIssueCount(), result.getIssueCount() - result.getCompletedIssueCount(),
                result.getCompletedPoints(), result.getTotalPoints() - result.getCompletedPoints());
    }

    public List<BurndownPointDTO> getBurndown(Integer sprintID) {
        requireSprint(sprintID);
        return progressRepository.findAllBySprintIdOrderByCapturedAtAscIdAsc(sprintID).stream()
                .map(point -> new BurndownPointDTO(point.getCapturedAt().toInstant(),
                        Math.max(0, point.getTotalPoints() - point.getCompletedPoints())))
                .toList();
    }

    public List<BurnupPointDTO> getBurnup(Integer sprintID) {
        requireSprint(sprintID);
        return progressRepository.findAllBySprintIdOrderByCapturedAtAscIdAsc(sprintID).stream()
                .map(point -> new BurnupPointDTO(point.getCapturedAt().toInstant(),
                        point.getCompletedPoints(), point.getTotalPoints()))
                .toList();
    }

    private Sprint requireSprint(Integer sprintID) {
        return sprintRepository.findById(requirePositiveId(sprintID))
                .orElseThrow(() -> new ResourceNotFoundException(SPRINT_DOES_NOT_EXISTS));
    }

    private Instant instant(ZonedDateTime date) {
        return date == null ? null : date.toInstant();
    }
}

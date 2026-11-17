package io.github.student.agile.resource.service;

import io.github.student.agile.exception.definition.ResourceNotFoundException;
import io.github.student.agile.exception.definition.StateConflictException;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintProgress;
import io.github.student.agile.resource.dto.BurndownPointDTO;
import io.github.student.agile.resource.dto.BurnupPointDTO;
import io.github.student.agile.resource.dto.SprintReportDTO;
import io.github.student.agile.resource.dto.request.ReportDateRange;
import io.github.student.agile.resource.repository.ProjectRepository;
import io.github.student.agile.resource.repository.SprintProgressRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.PROJECT_DOES_NOT_EXISTS;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.SPRINT_DOES_NOT_EXISTS;
import static io.github.student.agile.resource.service.RequestValidation.requirePositiveId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SprintReportService {

    private final SprintRepository sprintRepository;
    private final SprintProgressRepository progressRepository;
    private final ProjectRepository projectRepository;

    public SprintReportDTO getReport(Integer sprintID) {
        Sprint sprint = requireSprint(sprintID);
        if (!Boolean.TRUE.equals(sprint.getIsDone())) {
            throw new StateConflictException("Sprint report requires a closed sprint");
        }

        return createReport(sprint, progressRepository.findAllBySprintIdOrderByCapturedAtAscIdAsc(sprintID));
    }

    public List<SprintReportDTO> getComparison(Integer projectID, LocalDate from, LocalDate to) {
        requirePositiveId(projectID);
        ReportDateRange range = new ReportDateRange(from, to);
        if (!projectRepository.existsById(projectID)) {
            throw new ResourceNotFoundException(PROJECT_DOES_NOT_EXISTS);
        }
        List<Sprint> sprints = sprintRepository.findAllByProjectIdAndIsDoneTrue(projectID).stream()
                .filter(sprint -> matchesDateRange(sprint.getEndDate(), range))
                .sorted(Comparator.comparing(Sprint::getEndDate,
                        Comparator.nullsLast(Comparator.comparing(ZonedDateTime::toInstant)))
                        .thenComparing(Sprint::getId))
                .toList();
        if (sprints.isEmpty()) {
            return List.of();
        }
        Map<Integer, List<SprintProgress>> history = progressRepository
                .findAllBySprintIdInAndKindInOrderByCapturedAtAscIdAsc(
                        sprints.stream().map(Sprint::getId).toList(),
                        List.of(SprintProgress.Kind.START, SprintProgress.Kind.END))
                .stream().collect(Collectors.groupingBy(point -> point.getSprint().getId()));
        return sprints.stream().map(sprint -> createReport(sprint,
                history.getOrDefault(sprint.getId(), List.of()))).toList();
    }

    public String getComparisonCsv(Integer projectID, LocalDate from, LocalDate to) {
        List<SprintReportDTO> reports = getComparison(projectID, from, to);
        StringBuilder csv = new StringBuilder(
                "projectID,sprintID,name,startDate,endDate,completedIssueCount,incompleteIssueCount,velocity\r\n");
        for (SprintReportDTO report : reports) {
            csv.append(Stream.of(report.projectID(), report.sprintID(), report.name(),
                            report.startDate(), report.endDate(), report.completedIssueCount(),
                            report.incompleteIssueCount(), report.velocity())
                    .map(this::csvCell).collect(Collectors.joining(","))).append("\r\n");
        }
        return csv.toString();
    }

    private String csvCell(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private boolean matchesDateRange(ZonedDateTime endDate, ReportDateRange range) {
        if (range.from() == null && range.to() == null) {
            return true;
        }
        if (endDate == null) {
            return false;
        }
        LocalDate date = endDate.withZoneSameInstant(ZoneOffset.UTC).toLocalDate();
        return (range.from() == null || !date.isBefore(range.from()))
                && (range.to() == null || !date.isAfter(range.to()));
    }

    private SprintReportDTO createReport(Sprint sprint, List<SprintProgress> history) {
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

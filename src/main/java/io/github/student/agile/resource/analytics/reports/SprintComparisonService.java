
package io.github.student.agile.resource.analytics.reports;

import io.github.student.agile.resource.analytics.error.InvalidReportParameterException;
import io.github.student.agile.resource.analytics.error.ProjectNotFoundException;
import io.github.student.agile.resource.analytics.metrics.SprintMetrics;
import io.github.student.agile.resource.analytics.metrics.SprintMetricsProvider;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.repository.ProjectRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class SprintComparisonService {

    private static final ZoneId REPORT_ZONE =
            ZoneId.of("Europe/Warsaw");

    private final SprintComparisonRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final SprintMetricsProvider metricsProvider;

    public SprintComparisonService(
            SprintComparisonRepository sprintRepository,
            ProjectRepository projectRepository,
            SprintMetricsProvider metricsProvider) {
        this.sprintRepository = sprintRepository;
        this.projectRepository = projectRepository;
        this.metricsProvider = metricsProvider;
    }

    @Transactional(readOnly = true)
    public List<SprintComparisonResult> compare(
            Integer projectId, String from, String to) {

        LocalDate fromDate = parseDate(from, "from");
        LocalDate toDate = parseDate(to, "to");

        if (fromDate != null && toDate != null
                && fromDate.isAfter(toDate)) {
            throw new InvalidReportParameterException(
                    "Parameter 'from' cannot be after 'to'");
        }

        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }

        List<Sprint> sprints = new ArrayList<>(
                sprintRepository.findByProjectId(projectId)
        );

        sprints.removeIf(sprint ->
                !matchesFilter(sprint, fromDate, toDate));

        sprints.sort(
                Comparator.comparing(
                        Sprint::getStartDate,
                        Comparator.nullsLast(
                                Comparator.naturalOrder()
                        )
                ).thenComparing(Sprint::getId)
        );

        if (sprints.isEmpty()) {
            return List.of();
        }

        List<Integer> ids = sprints.stream()
                .map(Sprint::getId)
                .toList();

        Map<Integer, SprintMetrics> metrics =
                metricsProvider.forSprints(ids);

        return sprints.stream()
                .map(sprint -> new SprintComparisonResult(
                        sprint,
                        metrics.get(sprint.getId())
                ))
                .toList();
    }

    private LocalDate parseDate(String value, String name) {
        if (value == null) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new InvalidReportParameterException(
                    "Invalid date for parameter '" + name
                            + "'. Expected yyyy-MM-dd");
        }
    }

    private boolean matchesFilter(
            Sprint sprint, LocalDate from, LocalDate to) {

        if (from == null && to == null) {
            return true;
        }

        ZonedDateTime start = sprint.getStartDate();

        if (start == null) {
            return false;
        }

        LocalDate startDay = start.withZoneSameInstant(
                REPORT_ZONE).toLocalDate();

        ZonedDateTime end = sprint.getEndDate();

        LocalDate endDay = end == null
                ? null
                : end.withZoneSameInstant(
                        REPORT_ZONE).toLocalDate();

        return (to == null || !startDay.isAfter(to))
                && (endDay == null
                    || from == null
                    || !endDay.isBefore(from));
    }

    public record SprintComparisonResult(
            Sprint sprint,
            SprintMetrics metrics
    ) {
    }
}

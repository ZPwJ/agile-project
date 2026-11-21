
package io.github.student.agile.resource.analytics.reports;

import io.github.student.agile.resource.analytics.error.InvalidReportParameterException;
import io.github.student.agile.resource.analytics.error.ProjectNotFoundException;
import io.github.student.agile.resource.analytics.metrics.SprintMetrics;
import io.github.student.agile.resource.analytics.metrics.SprintMetricsProvider;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.dto.SprintComparisonRowDTO;
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
import java.util.Objects;

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
                        Objects.requireNonNull(
                                metrics.get(sprint.getId()),
                                "No metrics returned for sprint "
                                        + sprint.getId())
                ))
                .toList();
    }

    /**
     * Same as {@link #compare}, mapped to report rows. Sprint dates are
     * always expressed in Europe/Warsaw, whatever zone the database or the
     * JVM hands back.
     */
    @Transactional(readOnly = true)
    public List<SprintComparisonRowDTO> compareRows(
            Integer projectId, String from, String to) {

        return compare(projectId, from, to).stream()
                .map(result -> toRow(projectId, result))
                .toList();
    }

    private SprintComparisonRowDTO toRow(
            Integer projectId, SprintComparisonResult result) {

        Sprint sprint = result.sprint();
        SprintMetrics metrics = result.metrics();

        return new SprintComparisonRowDTO(
                sprint.getId(),
                sprint.getName(),
                projectId,
                inReportZone(sprint.getStartDate()),
                inReportZone(sprint.getEndDate()),
                metrics.state(),
                metrics.completedTasks(),
                metrics.incompleteTasks(),
                metrics.completedEstimate(),
                metrics.unit()
        );
    }

    private ZonedDateTime inReportZone(ZonedDateTime value) {
        return value == null
                ? null
                : value.withZoneSameInstant(REPORT_ZONE);
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


package io.github.student.agile.analytics;

import io.github.student.agile.resource.analytics.error.InvalidReportParameterException;
import io.github.student.agile.resource.analytics.error.ProjectNotFoundException;
import io.github.student.agile.resource.analytics.metrics.SprintMetrics;
import io.github.student.agile.resource.analytics.metrics.SprintMetricsProvider;
import io.github.student.agile.resource.analytics.reports.SprintComparisonRepository;
import io.github.student.agile.resource.analytics.reports.SprintComparisonService;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.repository.ProjectRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SprintComparisonServiceTest {

    @Mock
    private SprintComparisonRepository sprintRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private SprintMetricsProvider metricsProvider;

    private SprintComparisonService service;

    @BeforeEach
    void setUp() {
        service = new SprintComparisonService(
                sprintRepository,
                projectRepository,
                metricsProvider
        );
    }

    private Sprint sprint(int id, String start, String end) {
        return Sprint.builder()
                .id(id)
                .name("Sprint " + id)
                .startDate(start == null ? null : ZonedDateTime.parse(start))
                .endDate(end == null ? null : ZonedDateTime.parse(end))
                .build();
    }

    private void prepare(List<Sprint> sprints) {
        when(projectRepository.existsById(1)).thenReturn(true);
        when(sprintRepository.findByProjectId(1)).thenReturn(sprints);

        Map<Integer, SprintMetrics> metrics = new java.util.HashMap<>();

        for (Sprint sprint : sprints) {
            metrics.put(
                    sprint.getId(),
                    new SprintMetrics(
                            sprint.getId(),
                            null,
                            0, 0, 0, 0,
                            0L, 0L,
                            null
                    )
            );
        }

        lenient().when(metricsProvider.forSprints(anyCollection()))
                .thenReturn(metrics);
    }

    @Test
    void sortsSprintsByStartDate() {
        prepare(List.of(
                sprint(2, "2026-02-01T10:00:00+01:00", null),
                sprint(1, "2026-01-01T10:00:00+01:00", null)
        ));

        var result = service.compare(1, null, null);

        assertEquals(1, result.get(0).sprint().getId());
        assertEquals(2, result.get(1).sprint().getId());
    }

    @Test
    void nullStartDateGoesLast() {
        prepare(List.of(
                sprint(1, null, null),
                sprint(2, "2026-01-01T10:00:00+01:00", null)
        ));

        var result = service.compare(1, null, null);

        assertEquals(2, result.get(0).sprint().getId());
        assertEquals(1, result.get(1).sprint().getId());
    }

    @Test
    void filtersByDateRange() {
        prepare(List.of(
                sprint(1, "2026-01-01T10:00:00+01:00",
                        "2026-01-10T10:00:00+01:00"),
                sprint(2, "2026-03-01T10:00:00+01:00",
                        "2026-03-10T10:00:00+01:00")
        ));

        var result = service.compare(
                1, "2026-03-01", "2026-03-31");

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).sprint().getId());
    }

    @Test
    void openEndedSprintMatchesFilter() {
        prepare(List.of(
                sprint(1, "2026-01-01T10:00:00+01:00", null)
        ));

        var result = service.compare(
                1, "2026-03-01", "2026-03-31");

        assertEquals(1, result.size());
    }

    @Test
    void invalidDateRangeThrowsException() {
        assertThrows(
                InvalidReportParameterException.class,
                () -> service.compare(
                        1, "2026-12-31", "2026-01-01")
        );
    }

    @Test
    void invalidDateFormatThrowsException() {
        assertThrows(
                InvalidReportParameterException.class,
                () -> service.compare(
                        1, "zla-data", null)
        );
    }

    @Test
    void missingProjectThrowsException() {
        when(projectRepository.existsById(999)).thenReturn(false);

        assertThrows(
                ProjectNotFoundException.class,
                () -> service.compare(999, null, null)
        );
    }

    @Test
    void emptyProjectReturnsEmptyList() {
        prepare(List.of());

        var result = service.compare(1, null, null);

        assertTrue(result.isEmpty());
        verify(metricsProvider, never()).forSprints(anyCollection());
    }
}

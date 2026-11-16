package io.github.student.agile.service;

import io.github.student.agile.exception.definition.StateConflictException;
import io.github.student.agile.resource.domain.Project;
import io.github.student.agile.resource.domain.Sprint;
import io.github.student.agile.resource.domain.SprintProgress;
import io.github.student.agile.resource.dto.BurndownPointDTO;
import io.github.student.agile.resource.dto.SprintReportDTO;
import io.github.student.agile.resource.repository.SprintProgressRepository;
import io.github.student.agile.resource.repository.SprintRepository;
import io.github.student.agile.resource.service.SprintReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SprintReportServiceTest {

    @Mock private SprintRepository sprints;
    @Mock private SprintProgressRepository progress;
    @InjectMocks private SprintReportService service;

    private Sprint sprint;

    @BeforeEach
    void setUp() {
        sprint = Sprint.builder().id(1).name("Sprint").project(Project.builder().id(2).build())
                .isDone(true).startDate(ZonedDateTime.parse("2026-11-01T10:00:00+01:00"))
                .endDate(ZonedDateTime.parse("2026-11-10T10:00:00+01:00")).build();
        when(sprints.findById(1)).thenReturn(Optional.of(sprint));
    }

    @Test
    void usesStartCountAndFinalResultRatherThanIntermediateState() {
        when(progress.findAllBySprintIdOrderByCapturedAtAscIdAsc(1)).thenReturn(List.of(
                point(SprintProgress.Kind.START, 2, 0, 5, 0),
                point(SprintProgress.Kind.CHANGE, 3, 1, 10, 3),
                point(SprintProgress.Kind.END, 3, 2, 13, 8)));

        SprintReportDTO report = service.getReport(1);

        assertThat(report).isEqualTo(new SprintReportDTO(1, "Sprint", 2,
                sprint.getStartDate().toInstant(), sprint.getEndDate().toInstant(), 2, 2, 1, 8, 5));
    }

    @Test
    void emptySprintHasZeroCountsAndPoints() {
        when(progress.findAllBySprintIdOrderByCapturedAtAscIdAsc(1)).thenReturn(List.of(
                point(SprintProgress.Kind.START, 0, 0, 0, 0),
                point(SprintProgress.Kind.END, 0, 0, 0, 0)));

        SprintReportDTO report = service.getReport(1);

        assertThat(report.initialIssueCount()).isZero();
        assertThat(report.completedIssueCount()).isZero();
        assertThat(report.incompleteIssueCount()).isZero();
        assertThat(report.velocity()).isZero();
        assertThat(report.incompletePoints()).isZero();
    }

    @Test
    void missingStartHistoryAndDateRemainUnknown() {
        sprint.setStartDate(null);
        when(progress.findAllBySprintIdOrderByCapturedAtAscIdAsc(1))
                .thenReturn(List.of(point(SprintProgress.Kind.END, 2, 0, 5, 0)));

        SprintReportDTO report = service.getReport(1);

        assertThat(report.initialIssueCount()).isNull();
        assertThat(report.startDate()).isNull();
        assertThat(report.completedIssueCount()).isZero();
        assertThat(report.incompleteIssueCount()).isEqualTo(2);
        assertThat(report.velocity()).isZero();
        assertThat(report.incompletePoints()).isEqualTo(5);
    }

    @Test
    void refusesToInventFinalResultFromStartOrChange() {
        when(progress.findAllBySprintIdOrderByCapturedAtAscIdAsc(1)).thenReturn(List.of(
                point(SprintProgress.Kind.START, 1, 0, 3, 0),
                point(SprintProgress.Kind.CHANGE, 1, 1, 3, 3)));

        assertThatThrownBy(() -> service.getReport(1)).isInstanceOf(StateConflictException.class)
                .hasMessage("Sprint has no recorded final result");
    }

    @Test
    void burndownUsesOnlyRecordedTimestampsAndDoesNotInventMissingStart() {
        ZonedDateTime observed = sprint.getStartDate().plusDays(2);
        when(progress.findAllBySprintIdOrderByCapturedAtAscIdAsc(1)).thenReturn(List.of(
                SprintProgress.builder().kind(SprintProgress.Kind.CHANGE).capturedAt(observed)
                        .totalPoints(8).completedPoints(3).build(),
                SprintProgress.builder().kind(SprintProgress.Kind.END).capturedAt(sprint.getEndDate())
                        .totalPoints(8).completedPoints(8).build()));

        assertThat(service.getBurndown(1)).containsExactly(
                new BurndownPointDTO(observed.toInstant(), 5),
                new BurndownPointDTO(sprint.getEndDate().toInstant(), 0));
    }

    @Test
    void burndownNeverReturnsNegativeRemainingWork() {
        when(progress.findAllBySprintIdOrderByCapturedAtAscIdAsc(1)).thenReturn(List.of(
                SprintProgress.builder().kind(SprintProgress.Kind.END).capturedAt(sprint.getEndDate())
                        .totalPoints(2).completedPoints(3).build()));

        assertThat(service.getBurndown(1)).extracting(BurndownPointDTO::remainingPoints).containsExactly(0L);
    }

    private SprintProgress point(SprintProgress.Kind kind, int count, int done, long total, long completed) {
        return SprintProgress.builder().kind(kind).issueCount(count).completedIssueCount(done)
                .totalPoints(total).completedPoints(completed).build();
    }
}

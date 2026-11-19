package io.github.student.agile.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Raport podsumowujący pojedynczy sprint")
public class SprintSummaryReportDTO {

    @Schema(description = "Identyfikator sprintu", example = "1")
    private Integer sprintId;

    @Schema(description = "Nazwa sprintu", example = "Sprint 1")
    private String sprintName;

    @Schema(description = "Identyfikator projektu", example = "10")
    private Integer projectId;

    @Schema(description = "Data rozpoczęcia sprintu", nullable = true)
    private ZonedDateTime startDate;

    @Schema(description = "Data zakończenia sprintu (null dla sprintu aktywnego lub nieuruchomionego)", nullable = true)
    private ZonedDateTime endDate;

    @Schema(description = "Liczba zadań w sprincie w momencie jego rozpoczęcia", example = "5")
    private int initialIssueCount;

    @Schema(description = "Liczba zadań ukończonych (status DONE)", example = "3")
    private int completedIssueCount;

    @Schema(description = "Liczba zadań nieukończonych w sprincie", example = "2")
    private int uncompletedIssueCount;

    @Schema(description = "Velocity sprintu (suma estymacji zadań ukończonych; brak estymacji liczony jako 0)", example = "13")
    private int velocity;

    @Schema(description = "Suma estymacji zadań nieukończonych (brak estymacji liczony jako 0)", example = "8")
    private int uncompletedEstimationSum;

    @Schema(description = "Jednostka estymacji używana w metrykach", example = "STORY_POINTS")
    private String estimationUnit;
}

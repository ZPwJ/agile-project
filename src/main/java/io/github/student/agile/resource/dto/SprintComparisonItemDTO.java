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
@Schema(description = "Podsumowanie pojedynczego sprintu w zestawieniu porównawczym projektu")
public class SprintComparisonItemDTO {

    @Schema(description = "Identyfikator projektu", example = "10")
    private Integer projectId;

    @Schema(description = "Identyfikator sprintu", example = "1")
    private Integer sprintId;

    @Schema(description = "Nazwa sprintu", example = "Sprint 1")
    private String sprintName;

    @Schema(description = "Data rozpoczęcia sprintu", nullable = true)
    private ZonedDateTime startDate;

    @Schema(description = "Data zakończenia sprintu (null dla sprintu aktywnego)", nullable = true)
    private ZonedDateTime endDate;

    @Schema(description = "Liczba ukończonych zadań (status DONE)", example = "4")
    private int completedIssueCount;

    @Schema(description = "Liczba nieukończonych zadań w sprincie", example = "1")
    private int uncompletedIssueCount;

    @Schema(description = "Velocity sprintu w Story Points (0, gdy brak ukończonych zadań lub estymacji)", example = "13")
    private int velocity;
}

package io.github.student.agile.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Dane do wykresu Burnup sprintu prezentujące przyrost pracy ukończonej względem całkowitego zakresu sprintu")
public class BurnupChartDTO {

    @Schema(description = "Identyfikator sprintu", example = "1")
    private Integer sprintId;

    @Schema(description = "Nazwa sprintu", example = "Sprint 1")
    private String sprintName;

    @Schema(description = "Identyfikator projektu", example = "10")
    private Integer projectId;

    @Schema(description = "Data rozpoczęcia sprintu", nullable = true)
    private ZonedDateTime startDate;

    @Schema(description = "Data zakończenia sprintu (null dla sprintu aktywnego)", nullable = true)
    private ZonedDateTime endDate;

    @Schema(description = "Jednostka miary dla wszystkich wartości serii wykresu", example = "STORY_POINTS")
    private String unit;

    @Schema(description = "Chronologicznie uporządkowana seria punktów dziennych wykresu Burnup")
    private List<BurnupPointDTO> points;
}

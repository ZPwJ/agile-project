package io.github.student.agile.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Pojedynczy punkt dzienny na wykresie Burnup sprintu")
public class BurnupPointDTO {

    @Schema(description = "Data punktu na osi czasu wykresu (UTC, format ISO-8601 yyyy-MM-dd)", example = "2026-11-10")
    private LocalDate date;

    @Schema(description = "Skumulowana suma estymacji ukończonych zadań (status DONE) na koniec danego dnia (>= 0)", example = "8")
    private int completedWork;

    @Schema(description = "Całkowity zakres estymacji wszystkich zadań w sprincie na koniec danego dnia (>= 0)", example = "21")
    private int totalScope;

    @Schema(description = "Jednostka miary pracy i zakresu", example = "STORY_POINTS")
    private String unit;
}

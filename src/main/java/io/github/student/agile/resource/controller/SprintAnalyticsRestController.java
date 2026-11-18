package io.github.student.agile.resource.controller;

import io.github.student.agile.exception.dto.ApiErrorResponse;
import io.github.student.agile.resource.dto.SprintSummaryReportDTO;
import io.github.student.agile.resource.service.SprintAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Sprint Analytics", description = "Endpointy analityczne i raportowe dla sprintów Scrum")
public class SprintAnalyticsRestController {

    private final SprintAnalyticsService sprintAnalyticsService;

    @Operation(
            summary = "Pobierz raport podsumowujący sprint",
            description = "Zwraca podsumowanie pojedynczego sprintu (zakres początkowy, zadania ukończone i nieukończone, velocity oraz sumę estymacji nieukończonych w jednostce STORY_POINTS)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Raport podsumowujący sprint został wygenerowany",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SprintSummaryReportDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Niepoprawny parametr zapytania",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Brak lub nieprawidłowy token JWT",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Brak dostępu do projektu powiązanego ze sprintem",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Sprint o podanym identyfikatorze nie istnieje",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @GetMapping("/sprints/{sprintId}/report")
    public ResponseEntity<SprintSummaryReportDTO> getSprintSummaryReport(
            @Parameter(description = "Identyfikator sprintu", example = "1", required = true)
            @PathVariable Integer sprintId
    ) {
        return ResponseEntity.ok(sprintAnalyticsService.getSprintSummaryReport(sprintId));
    }
}

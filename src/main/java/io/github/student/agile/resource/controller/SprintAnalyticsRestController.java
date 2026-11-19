package io.github.student.agile.resource.controller;

import io.github.student.agile.exception.dto.ApiErrorResponse;
import io.github.student.agile.resource.dto.BurnupChartDTO;
import io.github.student.agile.resource.dto.SprintComparisonItemDTO;
import io.github.student.agile.resource.dto.SprintSummaryReportDTO;
import io.github.student.agile.resource.service.SprintAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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

import java.util.List;

import io.github.student.agile.utils.DateRangeParser;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.ZonedDateTime;

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

    @Operation(
            summary = "Pobierz dane do wykresu Burnup sprintu",
            description = "Zwraca chronologiczną serię punktów dziennych (date, completedWork, totalScope, unit) odzwierciedlającą skumulowaną pracę ukończoną (DONE) oraz całkowity zakres sprintu w jednostce STORY_POINTS."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Dane wykresu Burnup zostały wygenerowane",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BurnupChartDTO.class))
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
    @GetMapping("/sprints/{sprintId}/burnup")
    public ResponseEntity<BurnupChartDTO> getBurnupChart(
            @Parameter(description = "Identyfikator sprintu", example = "1", required = true)
            @PathVariable Integer sprintId
    ) {
        return ResponseEntity.ok(sprintAnalyticsService.getBurnupChart(sprintId));
    }

    @Operation(
            summary = "Pobierz zestawienie porównujące sprinty projektu",
            description = "Zwraca wszystkie uruchomione i zakończone sprinty wskazanego projektu w stabilnej kolejności (rosnąco po startDate, następnie sprintId), w tym sprinty o velocity równym 0."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Zestawienie sprintów projektu zostało wygenerowane",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = SprintComparisonItemDTO.class))
                    )
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
                    description = "Brak dostępu do wskazanego projektu",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Projekt o podanym identyfikatorze nie istnieje",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @GetMapping("/projects/{projectId}/sprints")
    public ResponseEntity<List<SprintComparisonItemDTO>> getProjectSprintsComparison(
            @Parameter(description = "Identyfikator projektu", example = "10", required = true)
            @PathVariable Integer projectId,
            @Parameter(description = "Początek przedziału (włącznie), ISO-8601: yyyy-MM-dd lub data z czasem. Brak = bez dolnej granicy", example = "2026-11-01")
            @RequestParam(required = false) String from,
            @Parameter(description = "Koniec przedziału (włącznie), ISO-8601: yyyy-MM-dd lub data z czasem. Brak = bez górnej granicy", example = "2026-11-30")
            @RequestParam(required = false) String to
    ) {
        ZonedDateTime fromDate = DateRangeParser.parseFrom(from);
        ZonedDateTime toDate = DateRangeParser.parseTo(to);
        return ResponseEntity.ok(sprintAnalyticsService.getProjectSprintsComparison(projectId, fromDate, toDate));
    }
}

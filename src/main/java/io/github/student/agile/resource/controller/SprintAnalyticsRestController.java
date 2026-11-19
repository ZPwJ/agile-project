package io.github.student.agile.resource.controller;

import io.github.student.agile.exception.dto.ApiErrorResponse;
import io.github.student.agile.resource.dto.BurndownChartDTO;
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
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Sprint Analytics", description = "Analytics and reporting endpoints for Scrum sprints")
public class SprintAnalyticsRestController {

    private final SprintAnalyticsService sprintAnalyticsService;

    @Operation(
            summary = "Get sprint summary report",
            description = "Returns a summary for a single sprint (initial issue count, completed and uncompleted issues, velocity, and uncompleted estimation sum in STORY_POINTS)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Sprint summary report generated",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SprintSummaryReportDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameter",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid JWT token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied to the project associated with the sprint",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Sprint with the given ID does not exist",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @GetMapping("/sprints/{sprintId}/report")
    public ResponseEntity<SprintSummaryReportDTO> getSprintSummaryReport(
            @Parameter(description = "Sprint identifier", example = "1", required = true)
            @PathVariable Integer sprintId
    ) {
        return ResponseEntity.ok(sprintAnalyticsService.getSprintSummaryReport(sprintId));
    }

    @Operation(
            summary = "Get sprint Burndown chart data",
            description = "Returns a chronological daily series of points (date, remainingWork, unit) representing remaining work in STORY_POINTS based on scope and status change history."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Burndown chart data generated",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BurndownChartDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameter",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid JWT token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied to the project associated with the sprint",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Sprint with the given ID does not exist",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @GetMapping("/sprints/{sprintId}/burndown")
    public ResponseEntity<BurndownChartDTO> getBurndownChart(
            @Parameter(description = "Sprint identifier", example = "1", required = true)
            @PathVariable Integer sprintId
    ) {
        return ResponseEntity.ok(sprintAnalyticsService.getBurndownChart(sprintId));
    }

    @Operation(
            summary = "Get sprint Burnup chart data",
            description = "Returns a chronological daily series of points (date, completedWork, totalScope, unit) representing cumulative completed work (DONE) and total sprint scope in STORY_POINTS."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Burnup chart data generated",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BurnupChartDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameter",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid JWT token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied to the project associated with the sprint",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Sprint with the given ID does not exist",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @GetMapping("/sprints/{sprintId}/burnup")
    public ResponseEntity<BurnupChartDTO> getBurnupChart(
            @Parameter(description = "Sprint identifier", example = "1", required = true)
            @PathVariable Integer sprintId
    ) {
        return ResponseEntity.ok(sprintAnalyticsService.getBurnupChart(sprintId));
    }

    @Operation(
            summary = "Get project sprints comparison",
            description = "Returns all started and completed sprints for the specified project in deterministic order (ascending by startDate, then sprintId), including sprints with velocity equal to 0."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Project sprints comparison generated",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = SprintComparisonItemDTO.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameter",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid JWT token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied to the specified project",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Project with the given ID does not exist",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @GetMapping("/projects/{projectId}/sprints")
    public ResponseEntity<List<SprintComparisonItemDTO>> getProjectSprintsComparison(
            @Parameter(description = "Project identifier", example = "10", required = true)
            @PathVariable Integer projectId,
            @Parameter(description = "Range start (inclusive), ISO-8601: yyyy-MM-dd or date-time. Empty means no lower bound", example = "2026-11-01")
            @RequestParam(required = false) String from,
            @Parameter(description = "Range end (inclusive), ISO-8601: yyyy-MM-dd or date-time. Empty means no upper bound", example = "2026-11-30")
            @RequestParam(required = false) String to
    ) {
        ZonedDateTime fromDate = DateRangeParser.parseFrom(from);
        ZonedDateTime toDate = DateRangeParser.parseTo(to);
        return ResponseEntity.ok(sprintAnalyticsService.getProjectSprintsComparison(projectId, fromDate, toDate));
    }

    @Operation(
            summary = "Export project sprints comparison to CSV",
            description = "Exports the sprints comparison for the specified project as an RFC 4180 CSV file, with optional date range filtering (from/to)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "CSV file with sprints comparison generated",
                    content = @Content(mediaType = "text/csv", schema = @Schema(type = "string", format = "binary"))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid query parameter or date range",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid JWT token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied to the specified project",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Project with the given ID does not exist",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @GetMapping("/projects/{projectId}/sprints/export")
    public ResponseEntity<byte[]> exportProjectSprintsComparisonCsv(
            @Parameter(description = "Project identifier", example = "10", required = true)
            @PathVariable Integer projectId,
            @Parameter(description = "Range start (inclusive), ISO-8601: yyyy-MM-dd or date-time. Empty means no lower bound", example = "2026-11-01")
            @RequestParam(required = false) String from,
            @Parameter(description = "Range end (inclusive), ISO-8601: yyyy-MM-dd or date-time. Empty means no upper bound", example = "2026-11-30")
            @RequestParam(required = false) String to
    ) {
        ZonedDateTime fromDate = DateRangeParser.parseFrom(from);
        ZonedDateTime toDate = DateRangeParser.parseTo(to);
        byte[] csvBytes = sprintAnalyticsService.exportProjectSprintsComparisonCsv(projectId, fromDate, toDate);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename("project-" + projectId + "-sprints.csv", StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csvBytes);
    }
}

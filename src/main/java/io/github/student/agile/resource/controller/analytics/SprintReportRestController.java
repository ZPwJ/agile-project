
package io.github.student.agile.resource.controller.analytics;

import io.github.student.agile.resource.analytics.reports.SprintComparisonService;
import io.github.student.agile.resource.analytics.reports.SprintComparisonCsvService;
import io.github.student.agile.resource.dto.SprintComparisonRowDTO;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/analytics/projects/{projectId}/sprints")
public class SprintReportRestController {

    private final SprintComparisonService comparisonService;
    private final SprintComparisonCsvService csvService;

    public SprintReportRestController(
            SprintComparisonService comparisonService,
            SprintComparisonCsvService csvService) {
        this.comparisonService = comparisonService;
        this.csvService = csvService;
    }

    @GetMapping("/comparison")
    public List<SprintComparisonRowDTO> comparison(
            @PathVariable Integer projectId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        return comparisonService.compare(projectId, from, to)
                .stream()
                .map(result -> {
                    var sprint = result.sprint();
                    var metrics = result.metrics();

                    return new SprintComparisonRowDTO(
                            sprint.getId(),
                            sprint.getName(),
                            projectId,
                            sprint.getStartDate(),
                            sprint.getEndDate(),
                            metrics.state(),
                            metrics.completedTasks(),
                            metrics.incompleteTasks(),
                            metrics.completedEstimate(),
                            metrics.unit()
                    );
                })
                .toList();
    }

    @GetMapping(
            value = "/comparison.csv",
            produces = "text/csv")
    public ResponseEntity<byte[]> comparisonCsv(
            @PathVariable Integer projectId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        List<SprintComparisonRowDTO> rows =
                comparison(projectId, from, to);

        byte[] csv = csvService.generate(rows);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"sprint-comparison-"
                                + projectId + ".csv\""
                )
                .contentType(
                        new MediaType(
                                "text",
                                "csv",
                                StandardCharsets.UTF_8
                        )
                )
                .body(csv);
    }
}

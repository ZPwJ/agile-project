package io.github.student.agile.resource.controller.analytics;

import io.github.student.agile.resource.analytics.reports.SprintComparisonCsvService;
import io.github.student.agile.resource.analytics.reports.SprintComparisonService;
import io.github.student.agile.resource.dto.SprintComparisonRowDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/analytics/projects/{projectId}/sprints")
@RequiredArgsConstructor
public class SprintReportRestController {

    private final SprintComparisonService comparisonService;
    private final SprintComparisonCsvService csvService;

    @GetMapping("/comparison")
    public List<SprintComparisonRowDTO> comparison(
            @PathVariable Integer projectId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        return comparisonService.compareRows(projectId, from, to);
    }

    @GetMapping(
            value = "/comparison.csv",
            produces = "text/csv")
    public ResponseEntity<byte[]> comparisonCsv(
            @PathVariable Integer projectId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        byte[] csv = csvService.generate(
                comparisonService.compareRows(projectId, from, to));

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

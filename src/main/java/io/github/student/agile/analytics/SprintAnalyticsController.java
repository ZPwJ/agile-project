package io.github.student.agile.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class SprintAnalyticsController {

    private final SprintAnalyticsService analyticsService;

    @GetMapping("/sprints/{sprintId}/report")
    public SprintReport getSprintReport(@PathVariable Integer sprintId) {
        return analyticsService.getSprintReport(sprintId);
    }

    @GetMapping("/projects/{projectId}/sprints")
    public List<SprintComparisonDTO> compareSprints(
            @PathVariable Integer projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analyticsService.compareSprints(projectId, from, to);
    }

    @GetMapping(value = "/projects/{projectId}/sprints/export.csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> exportSprintsCsv(
            @PathVariable Integer projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        String csv = analyticsService.exportSprintsCsv(projectId, from, to);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("project-" + projectId + "-sprints.csv")
                .build());
        return ResponseEntity.ok().headers(headers).body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
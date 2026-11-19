package io.github.student.agile.resource.controller.analytics;

import io.github.student.agile.resource.analytics.metrics.SprintReportService;
import io.github.student.agile.resource.dto.SprintSummaryDTO;
import io.github.student.agile.resource.dto.VelocityDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics/sprints")
@RequiredArgsConstructor
public class SprintMetricsRestController {

    private final SprintReportService sprintReportService;

    @GetMapping("/{sprintId}/summary")
    public ResponseEntity<SprintSummaryDTO> getSummary(@PathVariable Integer sprintId) {
        return ResponseEntity.ok(sprintReportService.getSummary(sprintId));
    }

    @GetMapping("/{sprintId}/velocity")
    public ResponseEntity<VelocityDTO> getVelocity(@PathVariable Integer sprintId) {
        return ResponseEntity.ok(sprintReportService.getVelocity(sprintId));
    }
}

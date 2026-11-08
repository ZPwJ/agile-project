package io.github.student.agile.resource.controller;

import io.github.student.agile.resource.dto.analytics.SprintReportDTO;
import io.github.student.agile.resource.service.SprintReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsRestController {

    private final SprintReportService sprintReportService;

    @GetMapping("/sprints/{sprintID}/report")
    public ResponseEntity<SprintReportDTO> getSprintReport(@PathVariable Integer sprintID) {
        return ResponseEntity.ok(sprintReportService.getSprintReport(sprintID));
    }
}

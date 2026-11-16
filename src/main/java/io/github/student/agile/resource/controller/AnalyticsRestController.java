package io.github.student.agile.resource.controller;

import io.github.student.agile.resource.dto.analytics.SprintBurndownDTO;
import io.github.student.agile.resource.dto.analytics.SprintBurnupDTO;
import io.github.student.agile.resource.dto.analytics.SprintComparisonDTO;
import io.github.student.agile.resource.dto.analytics.SprintReportDTO;
import io.github.student.agile.resource.service.SprintChartService;
import io.github.student.agile.resource.service.SprintComparisonService;
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
    private final SprintChartService sprintChartService;
    private final SprintComparisonService sprintComparisonService;

    @GetMapping("/sprints/{sprintID}/report")
    public ResponseEntity<SprintReportDTO> getSprintReport(@PathVariable Integer sprintID) {
        return ResponseEntity.ok(sprintReportService.getSprintReport(sprintID));
    }

    @GetMapping("/sprints/{sprintID}/burndown")
    public ResponseEntity<SprintBurndownDTO> getSprintBurndown(@PathVariable Integer sprintID) {
        return ResponseEntity.ok(sprintChartService.getBurndown(sprintID));
    }

    @GetMapping("/sprints/{sprintID}/burnup")
    public ResponseEntity<SprintBurnupDTO> getSprintBurnup(@PathVariable Integer sprintID) {
        return ResponseEntity.ok(sprintChartService.getBurnup(sprintID));
    }

    @GetMapping("/projects/{projectID}/sprints")
    public ResponseEntity<SprintComparisonDTO> compareProjectSprints(@PathVariable Integer projectID) {
        return ResponseEntity.ok(sprintComparisonService.compareSprints(projectID));
    }
}

package io.github.student.agile.resource.controller.analytics;

import io.github.student.agile.resource.analytics.charts.SprintChartsService;
import io.github.student.agile.resource.dto.BurndownDTO;
import io.github.student.agile.resource.dto.BurnupDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics/sprints")
@RequiredArgsConstructor
public class SprintChartsRestController {

    private final SprintChartsService chartsService;

    @GetMapping("/{sprintId}/burndown")
    public ResponseEntity<BurndownDTO> burndown(@PathVariable Integer sprintId) {
        return ResponseEntity.ok(chartsService.burndown(sprintId));
    }

    @GetMapping("/{sprintId}/burnup")
    public ResponseEntity<BurnupDTO> burnup(@PathVariable Integer sprintId) {
        return ResponseEntity.ok(chartsService.burnup(sprintId));
    }
}

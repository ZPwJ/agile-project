package io.github.student.agile.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
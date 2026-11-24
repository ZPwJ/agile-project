package io.github.student.agile.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class SprintAnalyticsController {

    private final SprintAnalyticsService analyticsService;

    @GetMapping("/sprints/{sprintId}/report")
    public SprintReport getSprintReport(@PathVariable Integer sprintId) {
        return analyticsService.getSprintReport(sprintId);
    }
}

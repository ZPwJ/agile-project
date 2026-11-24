package io.github.student.agile.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/sprints/{sprintId}/burndown")
    public List<BurndownPoint> getSprintBurndown(@PathVariable Integer sprintId) {
        return analyticsService.getSprintBurndown(sprintId);
    }
}

package io.github.student.agile.resource.controller;

import io.github.student.agile.resource.dto.BurndownPointDTO;
import io.github.student.agile.resource.dto.BurnupPointDTO;
import io.github.student.agile.resource.dto.SprintReportDTO;
import io.github.student.agile.resource.service.SprintReportService;
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
@RequestMapping("/api/reports/sprints")
@RequiredArgsConstructor
public class SprintReportRestController {

    private final SprintReportService reportService;

    @GetMapping
    public List<SprintReportDTO> getComparison(
            @RequestParam Integer projectID,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.getComparison(projectID, from, to);
    }

    @GetMapping("/{sprintID}")
    public SprintReportDTO getReport(@PathVariable Integer sprintID) {
        return reportService.getReport(sprintID);
    }

    @GetMapping("/{sprintID}/burndown")
    public List<BurndownPointDTO> getBurndown(@PathVariable Integer sprintID) {
        return reportService.getBurndown(sprintID);
    }

    @GetMapping("/{sprintID}/burnup")
    public List<BurnupPointDTO> getBurnup(@PathVariable Integer sprintID) {
        return reportService.getBurnup(sprintID);
    }
}

package io.github.student.agile.resource.service;

import io.github.student.agile.resource.dto.analytics.SprintReportDTO;

public interface SprintReportService {

    SprintReportDTO getSprintReport(Integer sprintID);
}

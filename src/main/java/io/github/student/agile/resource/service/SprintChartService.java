package io.github.student.agile.resource.service;

import io.github.student.agile.resource.dto.analytics.SprintBurndownDTO;
import io.github.student.agile.resource.dto.analytics.SprintBurnupDTO;

public interface SprintChartService {

    SprintBurndownDTO getBurndown(Integer sprintID);

    SprintBurnupDTO getBurnup(Integer sprintID);
}

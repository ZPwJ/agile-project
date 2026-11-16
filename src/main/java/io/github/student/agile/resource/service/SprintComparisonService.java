package io.github.student.agile.resource.service;

import io.github.student.agile.resource.dto.analytics.SprintComparisonDTO;

public interface SprintComparisonService {

    SprintComparisonDTO compareSprints(Integer projectID);
}

package io.github.student.agile.resource.dto.analytics;

import io.github.student.agile.resource.domain.Sprint;

import static java.lang.Boolean.TRUE;

public enum SprintState {

    PLANNED,
    ACTIVE,
    FINISHED;

    public static SprintState of(Sprint sprint) {
        if (TRUE.equals(sprint.getIsDone())) {
            return FINISHED;
        }
        return TRUE.equals(sprint.getActive()) ? ACTIVE : PLANNED;
    }
}

package io.github.student.agile.resource.analytics.error;

public class SprintNotFoundException extends RuntimeException {

    public SprintNotFoundException(Integer sprintId) {
        super("Sprint with ID " + sprintId + " does not exist");
    }
}

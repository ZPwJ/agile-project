package io.github.student.agile.resource.analytics.error;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(Integer projectId) {
        super("Project with ID " + projectId + " does not exist");
    }
}

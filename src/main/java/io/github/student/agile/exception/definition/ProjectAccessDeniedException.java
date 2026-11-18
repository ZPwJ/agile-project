package io.github.student.agile.exception.definition;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.PROJECT_ACCESS_DENIED;

public class ProjectAccessDeniedException extends RuntimeException {

    public ProjectAccessDeniedException() {
        super(PROJECT_ACCESS_DENIED);
    }

    public ProjectAccessDeniedException(String message) {
        super(message);
    }
}

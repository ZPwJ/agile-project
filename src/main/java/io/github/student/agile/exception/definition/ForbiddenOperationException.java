package io.github.student.agile.exception.definition;

/**
 * Thrown when the current user is not allowed to perform an operation; mapped to HTTP 403.
 */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}

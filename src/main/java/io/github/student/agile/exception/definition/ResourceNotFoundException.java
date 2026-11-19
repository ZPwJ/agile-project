package io.github.student.agile.exception.definition;

/**
 * Thrown when a requested resource (e.g. sprint or project) does not exist; mapped to HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

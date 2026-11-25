package io.github.student.agile.exception.definition;

/**
 * A resource (e.g. sprint or project) with the given identifier does not exist. Intended to map to HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

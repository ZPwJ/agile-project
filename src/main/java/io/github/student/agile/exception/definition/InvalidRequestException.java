package io.github.student.agile.exception.definition;

/**
 * Invalid request parameters (e.g. wrong date format or from > to). Intended to map to HTTP 400.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}

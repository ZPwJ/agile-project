package io.github.student.agile.exception.definition;

/**
 * Thrown when request parameters are invalid, e.g. a date range with from later than to; mapped to HTTP 400.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}

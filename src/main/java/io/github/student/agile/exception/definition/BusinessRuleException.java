package io.github.student.agile.exception.definition;

/**
 * Thrown when a request conflicts with the current state of a resource or a business rule; mapped to HTTP 409.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}

package io.github.student.agile.exception.definition;

/**
 * The operation violates a business rule (e.g. modifying a finished sprint). Intended to map to HTTP 409.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}

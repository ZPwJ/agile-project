package io.github.student.agile.exception.definition;

public class BadRequestException extends IllegalArgumentException {
    public BadRequestException(String message) {
        super(message);
    }
}

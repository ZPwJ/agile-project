package io.github.student.agile.exception.definition;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.INVALID_DATE_RANGE;

public class InvalidDateRangeException extends RuntimeException {

    public InvalidDateRangeException() {
        super(INVALID_DATE_RANGE);
    }

    public InvalidDateRangeException(String message) {
        super(message);
    }
}

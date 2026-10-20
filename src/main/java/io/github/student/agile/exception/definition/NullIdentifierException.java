package io.github.student.agile.exception.definition;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.NULL_IDENTIFIER;

public class NullIdentifierException extends IllegalArgumentException {

    public NullIdentifierException() {
        super(NULL_IDENTIFIER);
    }
}

package io.github.student.agile.resource.service;

import io.github.student.agile.exception.definition.BadRequestException;
import io.github.student.agile.exception.definition.NullIdentifierException;

public final class RequestValidation {
    private RequestValidation() { }

    public static Integer requirePositiveId(Integer id) {
        if (id == null) {
            throw new NullIdentifierException();
        }
        if (id <= 0) {
            throw new BadRequestException("Identifier must be positive");
        }
        return id;
    }

    public static void validateEstimation(Integer estimation) {
        if (estimation != null && estimation < 0) {
            throw new BadRequestException("Estimation cannot be negative");
        }
    }

    public static void requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Name is required");
        }
    }
}

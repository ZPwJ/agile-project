package io.github.student.agile.resource.dto.request;

import io.github.student.agile.exception.definition.BadRequestException;

import java.time.LocalDate;

public record ReportDateRange(LocalDate from, LocalDate to) {
    public ReportDateRange {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("from must not be after to");
        }
    }
}

package io.github.student.agile.resource.dto;

import java.time.LocalDate;

/** One day of a burndown chart: the date and the points still remaining at the end of that day. */
public record BurndownPointDTO(LocalDate date, long remaining) {
}

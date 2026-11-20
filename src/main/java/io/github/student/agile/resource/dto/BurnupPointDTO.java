package io.github.student.agile.resource.dto;

import java.time.LocalDate;

/** One day of a burnup chart: cumulative completed points and the sprint scope on that day. */
public record BurnupPointDTO(LocalDate date, long completed, long scope) {
}

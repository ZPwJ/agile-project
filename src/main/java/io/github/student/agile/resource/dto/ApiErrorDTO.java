package io.github.student.agile.resource.dto;

import java.time.Instant;

/**
 * Error body returned by every endpoint. Field names match Spring Boot's default error response,
 * so errors raised outside controllers (e.g. by security filters) keep the same structure.
 *
 * @param timestamp moment of the error, ISO-8601 in UTC
 * @param status    HTTP status code
 * @param error     HTTP reason phrase, e.g. "Not Found"
 * @param message   human-readable description, never a stack trace
 * @param path      request path
 */
public record ApiErrorDTO(Instant timestamp, int status, String error, String message, String path) {
}

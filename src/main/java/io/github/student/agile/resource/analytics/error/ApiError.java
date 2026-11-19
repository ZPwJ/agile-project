package io.github.student.agile.resource.analytics.error;

import java.time.OffsetDateTime;

/** Error body of the analytics API. Never contains stack traces or internal details. */
public record ApiError(String code, String message, OffsetDateTime timestamp) {
}

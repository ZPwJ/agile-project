package io.github.student.agile.resource.analytics.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;

/**
 * Maps analytics exceptions to 404/400. Limited to the analytics controllers so the behaviour of the
 * existing endpoints is unchanged.
 */
@RestControllerAdvice(basePackages = "io.github.student.agile.resource.controller.analytics")
public class AnalyticsExceptionHandler {

    @ExceptionHandler({SprintNotFoundException.class, ProjectNotFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(RuntimeException ex) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(InvalidReportParameterException.class)
    public ResponseEntity<ApiError> handleInvalidParameter(InvalidReportParameterException ex) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "Parameter '" + ex.getName() + "' has an invalid value");
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiError(code, message, OffsetDateTime.now()));
    }
}

package io.github.student.agile.resource.controller;

import io.github.student.agile.exception.definition.BadRequestException;
import io.github.student.agile.exception.definition.NullIdentifierException;
import io.github.student.agile.exception.definition.ResourceNotFoundException;
import io.github.student.agile.exception.definition.StateConflictException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    public record ApiError(int status, String message) { }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> notFound(ResourceNotFoundException exception) {
        return error(404, exception.getMessage());
    }

    @ExceptionHandler(StateConflictException.class)
    public ResponseEntity<ApiError> conflict(StateConflictException exception) {
        return error(409, exception.getMessage());
    }

    @ExceptionHandler({BadRequestException.class, NullIdentifierException.class})
    public ResponseEntity<ApiError> badRequest(IllegalArgumentException exception) {
        return error(400, exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> malformedRequest(Exception exception) {
        return error(400, "Invalid request parameters or JSON body");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> forbidden(AccessDeniedException exception) {
        return error(403, "Access denied");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception exception) {
        log.error("Unexpected API error", exception);
        return error(500, "Internal server error");
    }

    private ResponseEntity<ApiError> error(int status, String message) {
        return ResponseEntity.status(status).body(new ApiError(status, message));
    }
}

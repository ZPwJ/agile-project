package io.github.student.agile.resource.controller;

import io.github.student.agile.exception.definition.BusinessRuleException;
import io.github.student.agile.exception.definition.ForbiddenOperationException;
import io.github.student.agile.exception.definition.InvalidRequestException;
import io.github.student.agile.exception.definition.ResourceNotFoundException;
import io.github.student.agile.resource.dto.ApiErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

/**
 * Translates exceptions thrown by controllers into HTTP statuses with a uniform {@link ApiErrorDTO} body.
 * Messages of domain exceptions are returned as they are; unexpected exceptions are logged and answered
 * with a generic message, so internal details never reach the client.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    static final String UNEXPECTED_ERROR = "Unexpected server error";
    static final String MALFORMED_BODY = "Malformed request body";
    static final String ACCESS_DENIED = "Access denied";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorDTO> handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorDTO> handleBusinessRule(BusinessRuleException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ApiErrorDTO> handleForbidden(ForbiddenOperationException exception, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    /** Raised by {@code @PreAuthorize}; without this handler it would end up in the generic 500 handler. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorDTO> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, ACCESS_DENIED, request);
    }

    /** Covers {@link InvalidRequestException} and the existing {@code NullIdentifierException}. */
    @ExceptionHandler({InvalidRequestException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiErrorDTO> handleBadRequest(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ApiErrorDTO> handleTypeMismatch(TypeMismatchException exception, HttpServletRequest request) {
        String parameter = exception instanceof MethodArgumentTypeMismatchException mismatch
                ? mismatch.getName()
                : exception.getPropertyName();
        String message = "Invalid value '%s' for parameter '%s'".formatted(exception.getValue(), parameter);
        return error(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorDTO> handleMalformedBody(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, MALFORMED_BODY, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDTO> handleUnexpected(Exception exception, HttpServletRequest request) {
        // Standard Spring MVC exceptions (missing parameter, unsupported method, ...) carry their own status.
        if (exception instanceof ErrorResponse springError) {
            return error(springError.getStatusCode(), springError.getBody().getDetail(), request);
        }
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR, request);
    }

    private static ResponseEntity<ApiErrorDTO> error(HttpStatusCode status, String message, HttpServletRequest request) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        String reason = resolved == null ? String.valueOf(status.value()) : resolved.getReasonPhrase();
        ApiErrorDTO body = new ApiErrorDTO(Instant.now(), status.value(), reason, message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}

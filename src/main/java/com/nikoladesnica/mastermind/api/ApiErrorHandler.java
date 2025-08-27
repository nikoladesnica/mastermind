package com.nikoladesnica.mastermind.api;

import com.nikoladesnica.mastermind.domain.errors.BadRequestException;
import com.nikoladesnica.mastermind.domain.errors.ForbiddenException;
import com.nikoladesnica.mastermind.domain.errors.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.NoSuchElementException;

@RestControllerAdvice(basePackages = "com.nikoladesnica.mastermind")
public class ApiErrorHandler {

    // --- Domain exceptions -> typed HTTP status ---
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> badRequest(BadRequestException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> forbidden(ForbiddenException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI());
    }

    // --- Spring/validation -> 400 ---
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiError> validation(Exception ex, HttpServletRequest req) {
        String msg =
                ex instanceof MethodArgumentNotValidException manv
                        ? manv.getBindingResult().getFieldErrors().stream()
                        .findFirst().map(e -> e.getField() + " " + e.getDefaultMessage())
                        .orElse("Validation failed")
                        : ex instanceof ConstraintViolationException cve
                        ? cve.getConstraintViolations().stream()
                        .findFirst().map(v -> v.getPropertyPath() + " " + v.getMessage())
                        .orElse("Constraint violation")
                        : ex instanceof HttpMessageNotReadableException
                        ? "Malformed JSON"
                        : "Bad request";
        return build(HttpStatus.BAD_REQUEST, msg, req.getRequestURI());
    }

    // Explicit status exceptions
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> responseStatus(ResponseStatusException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        String msg = ex.getReason() != null ? ex.getReason() :
                (status != null ? status.getReasonPhrase() : "Error");
        return build(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR, msg, req.getRequestURI());
    }

    // Common repo-style 404
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> noSuch(NoSuchElementException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage() != null ? ex.getMessage() : "Not found", req.getRequestURI());
    }

    // Fallbacks
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> illegalArg(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage() != null ? ex.getMessage() : "Bad request", req.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> generic(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", req.getRequestURI());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, String path) {
        ApiError body = new ApiError(
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
        return ResponseEntity.status(status).body(body);
    }

    public record ApiError(
            String timestamp,
            int status,
            String error,
            String message,
            String path
    ) {}
}

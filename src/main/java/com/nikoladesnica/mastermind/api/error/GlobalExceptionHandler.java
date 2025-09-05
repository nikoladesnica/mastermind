package com.nikoladesnica.mastermind.api.error;

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

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Domain exceptions
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> badRequest(BadRequestException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> forbidden(ForbiddenException ex, HttpServletRequest req) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage(), req);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException ex, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    // Validation / format is 400
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiError> validation(Exception ex, HttpServletRequest req) {
        String msg;
        if (ex instanceof MethodArgumentNotValidException manv) {
            msg = manv.getBindingResult().getFieldErrors().stream()
                    .findFirst()
                    .map(e -> e.getField() + " " + e.getDefaultMessage())
                    .orElse("Validation failed");
        } else if (ex instanceof ConstraintViolationException cve) {
            msg = cve.getConstraintViolations().stream()
                    .findFirst()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .orElse("Constraint violation");
        } else if (ex instanceof HttpMessageNotReadableException) {
            msg = "Malformed JSON";
        } else {
            msg = "Bad request";
        }
        return error(HttpStatus.BAD_REQUEST, msg, req);
    }

    // Mirror ResponseStatusException
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> responseStatus(ResponseStatusException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        String msg = ex.getReason() != null
                ? ex.getReason()
                : (status != null ? status.getReasonPhrase() : "Error");
        return error(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR, msg, req);
    }

    // Common “repo not found” case
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> noSuch(NoSuchElementException ex, HttpServletRequest req) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Not found";
        return error(HttpStatus.NOT_FOUND, msg, req);
    }

    // Generic fallbacks
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> illegalArg(IllegalArgumentException ex, HttpServletRequest req) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Bad request";
        return error(HttpStatus.BAD_REQUEST, msg, req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> generic(Exception ex, HttpServletRequest req) {
        // Consider logging the exception server-side here
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", req);
    }

    // Helper
    private ResponseEntity<ApiError> error(HttpStatus status, String message, HttpServletRequest req) {
        ApiError body = new ApiError(
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                message,
                req.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }
}

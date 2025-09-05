package com.nikoladesnica.mastermind.api.error;

/** Stable error payload for all error responses. */
public record ApiError(
        String timestamp,  // Instant.now.toString() later...
        int status,        // 400, 403, 404, 500, ...
        String error,      // "Bad Request", "Forbidden", ...
        String message,
        String path        // request path
) {}
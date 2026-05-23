package com.tripgenie.common.api;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        String code,
        String message,
        int status,
        String path,
        Map<String, String> validationErrors,
        Instant timestamp
) {
    public static ErrorResponse of(String code, String message, int status, String path) {
        return new ErrorResponse(code, message, status, path, Map.of(), Instant.now());
    }

    public static ErrorResponse validation(String message, int status, String path, Map<String, String> errors) {
        return new ErrorResponse("VALIDATION_FAILED", message, status, path, errors, Instant.now());
    }
}

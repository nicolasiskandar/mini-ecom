package org.example.miniecom.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Standard API error payload")
public record ApiErrorResponse(
        @Schema(description = "Timestamp when the error occurred", example = "2026-02-13T09:30:12.531Z")
        Instant timestamp,
        @Schema(description = "HTTP status code", example = "400")
        int status,
        @Schema(description = "HTTP status reason", example = "Bad Request")
        String error,
        @Schema(description = "Human-readable message", example = "Validation failed")
        String message,
        @Schema(description = "Validation errors keyed by request field")
        Map<String, String> validationErrors
) {

    public static ApiErrorResponse of(int status, String error, String message, Map<String, String> validationErrors) {
        return new ApiErrorResponse(Instant.now(), status, error, message, validationErrors);
    }
}

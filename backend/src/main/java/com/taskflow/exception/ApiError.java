package com.taskflow.exception;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        int status,
        String message,
        Instant timestamp,
        Map<String, String> errors
) {

    public static ApiError of(int status, String message) {
        return new ApiError(status, message, Instant.now(), null);
    }

    public static ApiError of(int status, String message, Map<String, String> errors) {
        return new ApiError(status, message, Instant.now(), errors);
    }
}

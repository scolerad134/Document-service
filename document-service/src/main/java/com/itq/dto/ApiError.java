package com.itq.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    String code,
    String message,
    Instant timestamp,
    List<String> details
) {
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, Instant.now(), null);
    }

    public static ApiError of(String code, String message, List<String> details) {
        return new ApiError(code, message, Instant.now(), details);
    }
}

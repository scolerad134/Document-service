package com.itq.dto;

import com.itq.entity.enums.DocumentStatus;

import java.time.LocalDateTime;

public record DocumentResponse(
    Long id,
    String uniqueNumber,
    String author,
    DocumentStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}

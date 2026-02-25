package com.itq.dto;

import com.itq.entity.enums.DocumentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Документ")
public record DocumentResponse(
    @Schema(description = "Внутренний id") Long id,
    @Schema(description = "Уникальный номер (UUID)") String uniqueNumber,
    @Schema(description = "Автор") String author,
    @Schema(description = "Название") String title,
    @Schema(description = "DRAFT | SUBMITTED | APPROVED") DocumentStatus status,
    @Schema(description = "Дата создания") LocalDateTime createdAt,
    @Schema(description = "Дата обновления") LocalDateTime updatedAt) {}

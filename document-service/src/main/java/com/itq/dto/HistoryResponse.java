package com.itq.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Запись в истории документа")
public record HistoryResponse(
    @Schema(description = "SUBMIT | APPROVE") String action,
    @Schema(description = "Кто выполнил") String initiator,
    @Schema(description = "Комментарий") String comment,
    @Schema(description = "Когда") LocalDateTime createdAt
) {}

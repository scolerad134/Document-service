package com.itq.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Параметры теста конкурентного утверждения")
public record ConcurrentApprovalRequest(
    @Schema(description = "ID документа в статусе SUBMITTED", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Long documentId,
    @Schema(description = "Число потоков (1–100)", example = "5") @Min(1) @Max(100) int threads,
    @Schema(description = "Попыток на поток (1–100)", example = "10") @Min(1) @Max(100) int attempts,
    @Schema(description = "Инициатор", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String initiator
) {}

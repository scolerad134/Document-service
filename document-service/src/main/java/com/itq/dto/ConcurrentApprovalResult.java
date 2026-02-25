package com.itq.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Результат теста конкурентного утверждения")
public record ConcurrentApprovalResult(
    @Schema(description = "ID документа") Long documentId,
    @Schema(description = "Всего попыток") int totalAttempts,
    @Schema(description = "Успешных") int successCount,
    @Schema(description = "Конфликтов") int conflictCount,
    @Schema(description = "Ошибок") int errorCount,
    @Schema(description = "Финальный статус документа") String finalStatus,
    @Schema(description = "Пояснение") String message
) {}

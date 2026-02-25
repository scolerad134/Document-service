package com.itq.dto;

import com.itq.entity.enums.StatusChangeResultType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Результат операции по одному документу")
public record ItemResult(
    @Schema(description = "ID документа") Long id,
    @Schema(description = "SUCCESS | CONFLICT | NOT_FOUND | REGISTRY_ERROR") StatusChangeResultType result,
    @Schema(description = "Сообщение") String message
) {
}

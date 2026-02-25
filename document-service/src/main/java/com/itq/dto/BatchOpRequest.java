package com.itq.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Пакетная операция submit/approve")
public record BatchOpRequest(
    @Schema(description = "Инициатор действия", example = "user@company.com", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "Initiator cannot be blank") String initiator,
    @Schema(description = "Список id документов (1–1000)", requiredMode = Schema.RequiredMode.REQUIRED) @NotEmpty(message = "Ids cannot be empty") @Size(min = 1, max = 1000, message = "Ids count must be between 1 and 1000") List<Long> ids,
    @Schema(description = "Комментарий (опционально)") String comment
) {
}

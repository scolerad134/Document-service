package com.itq.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на создание документа")
public record DocumentCreateRequest(
    @Schema(description = "Инициатор создания", example = "user@company.com") @NotBlank(message = "Initiator cannot be blank") String initiator,
    @Schema(description = "Автор документа", example = "Иванов И.И.") @NotBlank(message = "Author cannot be blank") String author,
    @Schema(description = "Название документа", example = "Договор поставки") @NotBlank(message = "Title cannot be blank") String title
) {}

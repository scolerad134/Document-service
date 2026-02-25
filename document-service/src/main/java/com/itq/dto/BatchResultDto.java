package com.itq.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Результат пакетной операции")
public record BatchResultDto(@Schema(description = "Результат по каждому id") List<ItemResult> results) {
}

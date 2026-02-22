package com.itq.dto;

import com.itq.entity.enums.StatusChangeResultType;

public record StatusChangeResult(
    Long id,
    StatusChangeResultType result,
    String message
) {}
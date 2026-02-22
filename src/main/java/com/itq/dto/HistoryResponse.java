package com.itq.dto;

import java.time.LocalDateTime;

public record HistoryResponse(
    String action,
    String initiator,
    String comment,
    LocalDateTime createdAt
) {}

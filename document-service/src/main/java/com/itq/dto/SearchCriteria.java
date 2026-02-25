package com.itq.dto;

import java.time.LocalDateTime;

public record SearchCriteria(
    String status,
    String author,
    LocalDateTime fromDate,
    LocalDateTime toDate
) {}
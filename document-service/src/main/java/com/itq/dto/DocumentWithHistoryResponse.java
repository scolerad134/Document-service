package com.itq.dto;

import java.util.List;

public record DocumentWithHistoryResponse(
    DocumentResponse document,
    List<HistoryResponse> history
) {}

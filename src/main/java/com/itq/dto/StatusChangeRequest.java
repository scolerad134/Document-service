package com.itq.dto;

import java.util.List;

public record StatusChangeRequest(
    List<Long> ids,
    String initiator,
    String comment
) {}

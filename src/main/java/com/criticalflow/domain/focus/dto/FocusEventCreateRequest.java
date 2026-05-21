package com.criticalflow.domain.focus.dto;

import com.criticalflow.domain.focus.entity.FocusEvent;

import java.time.LocalDateTime;

public record FocusEventCreateRequest(
        FocusEvent.EventType eventType,
        LocalDateTime detectedAt,
        Integer durationSec
) {
}

package com.criticalflow.domain.focus.dto;

import com.criticalflow.domain.focus.entity.FocusEvent;

import java.time.LocalDateTime;

public record FocusEventResponse(
        Long eventId,
        Long sessionId,
        FocusEvent.EventType eventType,
        LocalDateTime detectedAt,
        Integer durationSec
) {
    public static FocusEventResponse from(FocusEvent event) {
        return new FocusEventResponse(
                event.getEventId(),
                event.getSessionId(),
                event.getEventType(),
                event.getDetectedAt(),
                event.getDurationSec()
        );
    }
}

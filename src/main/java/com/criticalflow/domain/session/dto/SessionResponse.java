package com.criticalflow.domain.session.dto;

import com.criticalflow.domain.session.entity.StudySession;

import java.time.LocalDateTime;

public record SessionResponse(
        Long sessionId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer totalStudyMinutes,
        Integer totalFocusMinutes
) {
    public static SessionResponse from(StudySession session) {
        return new SessionResponse(
                session.getSessionId(),
                session.getStartTime(),
                session.getEndTime(),
                session.getTotalStudyMinutes(),
                session.getTotalFocusMinutes()
        );
    }
}

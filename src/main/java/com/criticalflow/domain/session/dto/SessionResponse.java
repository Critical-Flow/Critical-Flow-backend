package com.criticalflow.domain.session.dto;

import com.criticalflow.domain.session.entity.StudySession;

import java.time.LocalDateTime;

public record SessionResponse(
        Long sessionId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer totalStudyMinutes,
        Integer totalFocusMinutes,
        Integer drowsyCount,
        Integer absentCount,
        Integer drowsySeconds,
        Integer absentSeconds
) {
    public static SessionResponse from(StudySession session) {
        return new SessionResponse(
                session.getSessionId(),
                session.getStartTime(),
                session.getEndTime(),
                session.getTotalStudyMinutes(),
                session.getTotalFocusMinutes(),
                session.getDrowsyCount(),
                session.getAbsentCount(),
                session.getDrowsySeconds(),
                session.getAbsentSeconds()
        );
    }
}

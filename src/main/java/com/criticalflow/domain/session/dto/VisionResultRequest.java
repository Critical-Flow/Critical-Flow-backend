package com.criticalflow.domain.session.dto;

public record VisionResultRequest(
        Long userId,
        Integer totalStudySeconds,
        Integer goodFocusSeconds,
        Integer drowsySeconds,
        Integer absentSeconds,
        Integer drowsyCount,
        Integer absentCount
) {}

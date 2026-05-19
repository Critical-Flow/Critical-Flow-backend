package com.criticalflow.domain.session.service;

import com.criticalflow.domain.focus.repository.FocusEventRepository;
import com.criticalflow.domain.session.dto.SessionResponse;
import com.criticalflow.domain.session.entity.StudySession;
import com.criticalflow.domain.session.repository.StudySessionRepository;
import com.criticalflow.global.exception.DomainException;
import com.criticalflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class StudySessionService {

    private final StudySessionRepository studySessionRepository;
    private final FocusEventRepository focusEventRepository;

    @Transactional
    public SessionResponse startSession(Long userId) {
        StudySession session = StudySession.builder()
                .userId(userId)
                .startTime(LocalDateTime.now())
                .build();
        return SessionResponse.from(studySessionRepository.save(session));
    }

    @Transactional
    public SessionResponse endSession(Long userId, Long sessionId) {
        StudySession session = studySessionRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new DomainException(ErrorCode.SESSION_NOT_FOUND));

        if (session.getEndTime() != null) {
            throw new DomainException(ErrorCode.SESSION_ALREADY_ENDED);
        }

        LocalDateTime endTime = LocalDateTime.now();
        int totalStudyMinutes = (int) ChronoUnit.MINUTES.between(session.getStartTime(), endTime);

        int lostSec = focusEventRepository.sumDurationSecBySessionId(sessionId);
        int lostMinutes = lostSec / 60;
        int totalFocusMinutes = Math.max(0, totalStudyMinutes - lostMinutes);

        session.end(endTime, totalStudyMinutes, totalFocusMinutes);
        return SessionResponse.from(session);
    }
}

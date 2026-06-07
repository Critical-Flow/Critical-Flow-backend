package com.criticalflow.domain.session.service;

import com.criticalflow.domain.session.dto.SessionResponse;
import com.criticalflow.domain.session.dto.VisionResultRequest;
import com.criticalflow.domain.session.entity.StudySession;
import com.criticalflow.domain.session.repository.StudySessionRepository;
import com.criticalflow.global.exception.DomainException;
import com.criticalflow.global.exception.ErrorCode;
import com.criticalflow.global.vision.PythonVisionClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudySessionService {

    private final StudySessionRepository studySessionRepository;
    private final PythonVisionClient pythonVisionClient;

    @Transactional
    public SessionResponse startSession(Long userId) {
        StudySession session = StudySession.builder()
                .userId(userId)
                .startTime(LocalDateTime.now())
                .build();
        StudySession saved = studySessionRepository.save(session);
        pythonVisionClient.startWebcam(saved.getSessionId(), userId);
        return SessionResponse.from(saved);
    }

    @Transactional
    public SessionResponse endSession(Long userId, Long sessionId) {
        StudySession session = studySessionRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new DomainException(ErrorCode.SESSION_NOT_FOUND));

        if (session.getEndTime() != null) {
            throw new DomainException(ErrorCode.SESSION_ALREADY_ENDED);
        }

        session.end(LocalDateTime.now());
        pythonVisionClient.stopWebcam(sessionId);
        return SessionResponse.from(session);
    }

    @Transactional(readOnly = true)
    public Optional<SessionResponse> getActiveSession(Long userId) {
        return studySessionRepository
                .findFirstByUserIdAndEndTimeIsNullOrderByStartTimeDesc(userId)
                .map(SessionResponse::from);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getSessions(Long userId) {
        return studySessionRepository.findByUserIdOrderByStartTimeDesc(userId)
                .stream()
                .map(SessionResponse::from)
                .toList();
    }

    @Transactional
    public SessionResponse applyVisionResult(Long sessionId, VisionResultRequest request) {
        StudySession session = studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new DomainException(ErrorCode.SESSION_NOT_FOUND));

        session.applyVisionResult(
                request.totalStudySeconds(),
                request.goodFocusSeconds(),
                request.drowsySeconds(),
                request.absentSeconds(),
                request.drowsyCount(),
                request.absentCount()
        );
        return SessionResponse.from(session);
    }
}

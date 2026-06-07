package com.criticalflow.domain.focus.service;

import com.criticalflow.domain.focus.dto.FocusEventCreateRequest;
import com.criticalflow.domain.focus.dto.FocusEventResponse;
import com.criticalflow.domain.focus.entity.FocusEvent;
import com.criticalflow.domain.focus.repository.FocusEventRepository;
import com.criticalflow.domain.session.entity.StudySession;
import com.criticalflow.domain.session.repository.StudySessionRepository;
import com.criticalflow.global.exception.DomainException;
import com.criticalflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FocusEventService {

    private final FocusEventRepository focusEventRepository;
    private final StudySessionRepository studySessionRepository;

    @Transactional
    public FocusEventResponse createEvent(Long sessionId, FocusEventCreateRequest request) {
        if (!studySessionRepository.existsById(sessionId)) {
            throw new DomainException(ErrorCode.SESSION_NOT_FOUND);
        }
        FocusEvent event = FocusEvent.builder()
                .sessionId(sessionId)
                .eventType(request.eventType())
                .detectedAt(request.detectedAt())
                .durationSec(request.durationSec())
                .alerted(false)
                .build();
        return FocusEventResponse.from(focusEventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public List<FocusEventResponse> getEventsBySession(Long userId, Long sessionId) {
        validateSessionOwnership(userId, sessionId);

        return focusEventRepository.findBySessionIdOrderByDetectedAtAsc(sessionId)
                .stream()
                .map(FocusEventResponse::from)
                .toList();
    }

    private void validateSessionOwnership(Long userId, Long sessionId) {
        StudySession session = studySessionRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new DomainException(ErrorCode.SESSION_NOT_FOUND));
    }
}

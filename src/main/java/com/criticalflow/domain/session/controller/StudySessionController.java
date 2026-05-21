package com.criticalflow.domain.session.controller;

import com.criticalflow.domain.session.dto.SessionResponse;
import com.criticalflow.domain.session.service.StudySessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class StudySessionController {

    private final StudySessionService studySessionService;

    @PostMapping
    public ResponseEntity<SessionResponse> startSession(
            @AuthenticationPrincipal Long userId
    ) {
        SessionResponse response = studySessionService.startSession(userId);
        return ResponseEntity
                .created(URI.create("/api/v1/sessions/" + response.sessionId()))
                .body(response);
    }

    @PostMapping("/{sessionId}/end")
    public ResponseEntity<SessionResponse> endSession(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(studySessionService.endSession(userId, sessionId));
    }
}

package com.criticalflow.domain.focus.controller;

import com.criticalflow.domain.focus.dto.FocusEventCreateRequest;
import com.criticalflow.domain.focus.dto.FocusEventResponse;
import com.criticalflow.domain.focus.service.FocusEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/focus-events")
@RequiredArgsConstructor
public class FocusEventController {

    private final FocusEventService focusEventService;

    @PostMapping
    public ResponseEntity<FocusEventResponse> recordEvent(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId,
            @RequestBody FocusEventCreateRequest request
    ) {
        FocusEventResponse response = focusEventService.recordEvent(userId, sessionId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/sessions/" + sessionId + "/focus-events/" + response.eventId()))
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<FocusEventResponse>> getEvents(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(focusEventService.getEventsBySession(userId, sessionId));
    }
}

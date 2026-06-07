package com.criticalflow.domain.focus.controller;

import com.criticalflow.domain.focus.dto.FocusEventCreateRequest;
import com.criticalflow.domain.focus.dto.FocusEventResponse;
import com.criticalflow.domain.focus.service.FocusEventService;
import com.criticalflow.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import java.util.List;

@Tag(name = "Focus Event", description = "집중도 이벤트 조회 API")
@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/focus-events")
@RequiredArgsConstructor
public class FocusEventController {

    private final FocusEventService focusEventService;

    @Operation(
            summary = "집중 이탈 이벤트 저장",
            description = "Python AI 서버 또는 ESP32에서 감지한 집중 이탈 이벤트를 저장합니다. 내부 서버 전용 API입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "이벤트 저장 성공"),
            @ApiResponse(responseCode = "400", description = "필수 필드 누락 (eventType, durationSec)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"INVALID_REQUEST_BODY\",\"message\":\"요청 데이터가 제약 조건을 위반했습니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"SESSION_NOT_FOUND\",\"message\":\"학습 세션을 찾을 수 없습니다.\"}")))
    })
    @PostMapping
    public ResponseEntity<FocusEventResponse> createEvent(
            @Parameter(description = "이벤트를 기록할 세션 ID", required = true) @PathVariable Long sessionId,
            @RequestBody FocusEventCreateRequest request
    ) {
        FocusEventResponse response = focusEventService.createEvent(sessionId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/sessions/" + sessionId + "/focus-events"))
                .body(response);
    }

    @Operation(
            summary = "집중도 이벤트 목록 조회",
            description = "특정 세션의 집중이탈 이벤트를 detectedAt 오름차순으로 반환합니다. 대시보드 용도.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"INVALID_ACCESS_TOKEN\",\"message\":\"유효하지 않은 AccessToken입니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"SESSION_NOT_FOUND\",\"message\":\"학습 세션을 찾을 수 없습니다.\"}")))
    })
    @GetMapping
    public ResponseEntity<List<FocusEventResponse>> getEvents(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "조회할 세션 ID", required = true) @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(focusEventService.getEventsBySession(userId, sessionId));
    }
}

package com.criticalflow.domain.session.controller;

import com.criticalflow.domain.session.dto.SessionResponse;
import com.criticalflow.domain.session.service.StudySessionService;
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

@Tag(name = "Study Session", description = "학습 세션 시작·종료 API")
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class StudySessionController {

    private final StudySessionService studySessionService;

    @Operation(
            summary = "학습 세션 시작",
            description = "새 학습 세션을 시작합니다. 현재 시각이 startTime으로 기록됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "세션 시작 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"INVALID_ACCESS_TOKEN\",\"message\":\"유효하지 않은 AccessToken입니다.\"}")))
    })
    @PostMapping
    public ResponseEntity<SessionResponse> startSession(
            @AuthenticationPrincipal Long userId
    ) {
        SessionResponse response = studySessionService.startSession(userId);
        return ResponseEntity
                .created(URI.create("/api/v1/sessions/" + response.sessionId()))
                .body(response);
    }

    @Operation(
            summary = "학습 세션 종료",
            description = "세션을 종료하고 totalStudyMinutes·totalFocusMinutes를 자동 계산합니다. "
                    + "totalFocusMinutes = totalStudyMinutes - 집중이탈 이벤트 총 durationSec(분 환산), 최솟값 0.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "세션 종료 성공"),
            @ApiResponse(responseCode = "400", description = "이미 종료된 세션",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"SESSION_ALREADY_ENDED\",\"message\":\"이미 종료된 세션입니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"INVALID_ACCESS_TOKEN\",\"message\":\"유효하지 않은 AccessToken입니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"SESSION_NOT_FOUND\",\"message\":\"학습 세션을 찾을 수 없습니다.\"}")))
    })
    @PostMapping("/{sessionId}/end")
    public ResponseEntity<SessionResponse> endSession(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "종료할 세션 ID", required = true) @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(studySessionService.endSession(userId, sessionId));
    }
}

package com.criticalflow.domain.session.controller;

import com.criticalflow.domain.session.dto.SessionResponse;
import com.criticalflow.domain.session.dto.VisionResultRequest;
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
import java.util.List;

@Tag(name = "Study Session", description = "학습 세션 시작·종료 API")
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class StudySessionController {

    private final StudySessionService studySessionService;

    @Operation(
            summary = "세션 목록 조회",
            description = "현재 로그인한 사용자의 전체 세션 목록을 최신순으로 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"INVALID_ACCESS_TOKEN\",\"message\":\"유효하지 않은 AccessToken입니다.\"}")))
    })
    @GetMapping
    public ResponseEntity<List<SessionResponse>> getSessions(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(studySessionService.getSessions(userId));
    }

    @Operation(
            summary = "현재 진행 중인 세션 조회",
            description = "종료되지 않은 세션이 있으면 반환합니다. 없으면 204 No Content를 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "진행 중인 세션 있음"),
            @ApiResponse(responseCode = "204", description = "진행 중인 세션 없음"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"INVALID_ACCESS_TOKEN\",\"message\":\"유효하지 않은 AccessToken입니다.\"}")))
    })
    @GetMapping("/active")
    public ResponseEntity<SessionResponse> getActiveSession(
            @AuthenticationPrincipal Long userId
    ) {
        return studySessionService.getActiveSession(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

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
            description = "세션 종료 시각을 기록하고 Python 웹캠 종료 신호를 전송합니다. "
                    + "집계 데이터(totalStudyMinutes 등)는 Python 분석 완료 후 /vision-result 콜백으로 업데이트됩니다.",
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

    @Operation(
            summary = "Python 비전 분석 결과 수신",
            description = "Python 웹캠 분석 완료 후 집계 결과를 수신합니다. Python 서버에서만 호출합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결과 저장 성공"),
            @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"SESSION_NOT_FOUND\",\"message\":\"학습 세션을 찾을 수 없습니다.\"}")))
    })
    @PostMapping("/{sessionId}/vision-result")
    public ResponseEntity<SessionResponse> receiveVisionResult(
            @Parameter(description = "세션 ID", required = true) @PathVariable Long sessionId,
            @RequestBody VisionResultRequest request
    ) {
        return ResponseEntity.ok(studySessionService.applyVisionResult(sessionId, request));
    }
}

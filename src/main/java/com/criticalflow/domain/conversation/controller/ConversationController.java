package com.criticalflow.domain.conversation.controller;

import com.criticalflow.domain.conversation.entity.AiConversation;
import com.criticalflow.domain.conversation.dto.ConversationResponse;
import com.criticalflow.domain.conversation.dto.MessageResponse;
import com.criticalflow.domain.conversation.dto.SendMessageRequest;
import com.criticalflow.domain.conversation.dto.StartConversationRequest;
import com.criticalflow.domain.conversation.service.ConversationService;
import com.criticalflow.global.ai.tutor.AiTutorService;
import com.criticalflow.global.ai.tutor.TutorResponse;
import com.criticalflow.global.exception.DomainException;
import com.criticalflow.global.exception.ErrorCode;
import com.criticalflow.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Conversation", description = "AI 튜터 대화 API")
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final AiTutorService aiTutorService;

    @Operation(summary = "대화 시작", description = "노트를 기반으로 AI 튜터 대화를 시작합니다. 첫 질문이 자동 생성됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "대화 시작 성공"),
            @ApiResponse(responseCode = "404", description = "노트를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"CONVERSATION_NOTE_NOT_FOUND\",\"message\":\"대화에 연결된 노트를 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "AI 응답 생성 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"AI_RESPONSE_FAILED\",\"message\":\"AI 응답 생성에 실패했습니다.\"}")))
    })
    @PostMapping
    public ResponseEntity<ConversationResponse> start(@RequestBody StartConversationRequest request) {
        AiConversation conversation = conversationService.start(
                request.noteId(), request.userId(), request.type(), request.questionType()
        );
        TutorResponse firstQuestion = aiTutorService.generateFirstQuestion(conversation.getConversationId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ConversationResponse.from(conversation, firstQuestion.getContent()));
    }

    @Operation(summary = "메시지 전송", description = "사용자 메시지를 전송하고 AI 튜터 응답을 받습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메시지 전송 및 AI 응답 성공"),
            @ApiResponse(responseCode = "400", description = "메시지 내용 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"MESSAGE_EMPTY\",\"message\":\"메시지 내용은 필수입니다.\"}"))),
            @ApiResponse(responseCode = "404", description = "대화를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"CONVERSATION_NOT_FOUND\",\"message\":\"대화를 찾을 수 없습니다.\"}"))),
            @ApiResponse(responseCode = "500", description = "AI 응답 생성 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"AI_RESPONSE_FAILED\",\"message\":\"AI 응답 생성에 실패했습니다.\"}")))
    })
    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<TutorResponse> sendMessage(
            @Parameter(description = "대화 ID", required = true) @PathVariable Long conversationId,
            @RequestBody SendMessageRequest request) {
        if (request.userMessage() == null || request.userMessage().isBlank()) {
            throw new DomainException(ErrorCode.MESSAGE_EMPTY);
        }
        TutorResponse response = aiTutorService.respond(conversationId, request.userMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "대화 메시지 목록 조회", description = "특정 대화의 전체 메시지 기록을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "대화를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\":\"CONVERSATION_NOT_FOUND\",\"message\":\"대화를 찾을 수 없습니다.\"}")))
    })
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @Parameter(description = "대화 ID", required = true) @PathVariable Long conversationId) {
        List<MessageResponse> messages = conversationService.getMessages(conversationId)
                .stream()
                .map(MessageResponse::from)
                .toList();
        return ResponseEntity.ok(messages);
    }
}

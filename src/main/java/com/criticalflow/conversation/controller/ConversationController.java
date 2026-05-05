package com.criticalflow.conversation.controller;

import com.criticalflow.conversation.dto.ConversationResponse;
import com.criticalflow.conversation.dto.MessageResponse;
import com.criticalflow.conversation.dto.SendMessageRequest;
import com.criticalflow.conversation.dto.StartConversationRequest;
import com.criticalflow.conversation.service.ConversationService;
import com.criticalflow.domain.ai.entity.AiConversation;
import com.criticalflow.global.ai.tutor.AiTutorService;
import com.criticalflow.global.ai.tutor.TutorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final AiTutorService aiTutorService;

    /**
     * 새 대화 세션을 시작한다.
     * noteId, userId, type(QUESTION|QUIZ)를 받아 conversation을 생성하고 conversationId를 반환.
     */
    @PostMapping
    public ResponseEntity<ConversationResponse> start(@RequestBody StartConversationRequest request) {
        AiConversation conversation = conversationService.start(
                request.noteId(), request.userId(), request.type()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ConversationResponse.from(conversation));
    }

    /**
     * 학습자 메시지를 전송하고 AI 튜터의 응답을 받는다.
     * AiTutorService가 RAG + FocusEvent + System Prompt를 조합해 응답을 생성한다.
     */
    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<TutorResponse> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody SendMessageRequest request) {
        TutorResponse response = aiTutorService.respond(conversationId, request.userMessage());
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 대화의 전체 메시지 히스토리를 시간순으로 반환한다.
     */
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(@PathVariable Long conversationId) {
        List<MessageResponse> messages = conversationService.getMessages(conversationId)
                .stream()
                .map(MessageResponse::from)
                .toList();
        return ResponseEntity.ok(messages);
    }
}

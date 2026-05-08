package com.criticalflow.domain.conversation.controller;

import com.criticalflow.domain.conversation.entity.AiConversation;
import com.criticalflow.domain.conversation.dto.ConversationResponse;
import com.criticalflow.domain.conversation.dto.MessageResponse;
import com.criticalflow.domain.conversation.dto.SendMessageRequest;
import com.criticalflow.domain.conversation.dto.StartConversationRequest;
import com.criticalflow.domain.conversation.service.ConversationService;
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

    @PostMapping
    public ResponseEntity<ConversationResponse> start(@RequestBody StartConversationRequest request) {
        AiConversation conversation = conversationService.start(
                request.noteId(), request.userId(), request.type(), request.questionType()
        );
        TutorResponse firstQuestion = aiTutorService.generateFirstQuestion(conversation.getConversationId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ConversationResponse.from(conversation, firstQuestion.getContent()));
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<TutorResponse> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody SendMessageRequest request) {
        TutorResponse response = aiTutorService.respond(conversationId, request.userMessage());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(@PathVariable Long conversationId) {
        List<MessageResponse> messages = conversationService.getMessages(conversationId)
                .stream()
                .map(MessageResponse::from)
                .toList();
        return ResponseEntity.ok(messages);
    }
}

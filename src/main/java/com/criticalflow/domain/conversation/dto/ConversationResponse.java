package com.criticalflow.domain.conversation.dto;

import com.criticalflow.domain.conversation.entity.AiConversation;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ConversationResponse {

    private Long conversationId;
    private Long noteId;
    private Long userId;
    private String type;
    private LocalDateTime createdAt;
    private String firstQuestion;

    public static ConversationResponse from(AiConversation conversation, String firstQuestion) {
        return ConversationResponse.builder()
                .conversationId(conversation.getConversationId())
                .noteId(conversation.getNoteId())
                .userId(conversation.getUserId())
                .type(conversation.getType().name())
                .createdAt(conversation.getCreatedAt())
                .firstQuestion(firstQuestion)
                .build();
    }
}

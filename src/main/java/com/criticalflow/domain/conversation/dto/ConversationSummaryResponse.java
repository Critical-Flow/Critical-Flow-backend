package com.criticalflow.domain.conversation.dto;

import com.criticalflow.domain.conversation.entity.AiConversation;
import com.criticalflow.domain.conversation.entity.QuestionType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ConversationSummaryResponse {

    private Long conversationId;
    private Long noteId;
    private String type;
    private QuestionType questionType;
    private LocalDateTime createdAt;

    public static ConversationSummaryResponse from(AiConversation conversation) {
        return ConversationSummaryResponse.builder()
                .conversationId(conversation.getConversationId())
                .noteId(conversation.getNoteId())
                .type(conversation.getType().name())
                .questionType(conversation.getQuestionType())
                .createdAt(conversation.getCreatedAt())
                .build();
    }
}

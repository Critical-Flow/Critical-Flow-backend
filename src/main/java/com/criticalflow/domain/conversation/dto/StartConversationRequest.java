package com.criticalflow.domain.conversation.dto;

import com.criticalflow.domain.conversation.entity.AiConversation;
import com.criticalflow.domain.conversation.entity.QuestionType;

public record StartConversationRequest(
        Long noteId,
        Long userId,
        AiConversation.ConversationType type,
        QuestionType questionType
) {
}

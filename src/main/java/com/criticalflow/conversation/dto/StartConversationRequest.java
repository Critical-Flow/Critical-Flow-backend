package com.criticalflow.conversation.dto;

import com.criticalflow.domain.ai.entity.AiConversation;
import com.criticalflow.domain.ai.entity.QuestionType;

public record StartConversationRequest(
        Long noteId,
        Long userId,
        AiConversation.ConversationType type,
        QuestionType questionType
) {
}

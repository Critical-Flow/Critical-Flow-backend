package com.criticalflow.conversation.dto;

import com.criticalflow.domain.ai.entity.AiConversation;

public record StartConversationRequest(
        Long noteId,
        Long userId,
        AiConversation.ConversationType type
) {
}

package com.criticalflow.domain.conversation.dto;

import com.criticalflow.domain.ai.entity.AiMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MessageResponse {

    private Long messageId;
    private String role;
    private String content;
    private Integer sequence;
    private LocalDateTime createdAt;

    public static MessageResponse from(AiMessage message) {
        return MessageResponse.builder()
                .messageId(message.getMessageId())
                .role(message.getRole().name())
                .content(message.getContent())
                .sequence(message.getSequence())
                .createdAt(message.getCreatedAt())
                .build();
    }
}

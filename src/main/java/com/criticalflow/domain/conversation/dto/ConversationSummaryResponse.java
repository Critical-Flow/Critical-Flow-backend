package com.criticalflow.domain.conversation.dto;

import com.criticalflow.domain.conversation.entity.AiConversation;
import com.criticalflow.domain.conversation.entity.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "대화 요약 응답")
public class ConversationSummaryResponse {

    @Schema(description = "대화 ID", example = "1")
    private Long conversationId;

    @Schema(description = "연결된 노트 ID", example = "5")
    private Long noteId;

    @Schema(description = "대화 유형 (QUESTION | QUIZ)", example = "QUESTION")
    private String type;

    @Schema(description = "질문 유형 (TYPE_A ~ TYPE_F, QUIZ일 때 null 가능)", example = "TYPE_A", nullable = true)
    private QuestionType questionType;

    @Schema(description = "대화 생성 시각", example = "2026-05-31T10:00:00")
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

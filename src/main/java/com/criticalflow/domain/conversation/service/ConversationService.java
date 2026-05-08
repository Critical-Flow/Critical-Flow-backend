package com.criticalflow.domain.conversation.service;

import com.criticalflow.domain.ai.entity.AiConversation;
import com.criticalflow.domain.ai.entity.AiMessage;
import com.criticalflow.domain.ai.entity.QuestionType;
import com.criticalflow.domain.ai.repository.AiConversationRepository;
import com.criticalflow.domain.ai.repository.AiMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;

    @Transactional
    public AiConversation start(Long noteId, Long userId, AiConversation.ConversationType type,
                                QuestionType questionType) {
        return conversationRepository.save(AiConversation.builder()
                .noteId(noteId)
                .userId(userId)
                .type(type)
                .questionType(questionType)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional(readOnly = true)
    public List<AiMessage> getMessages(Long conversationId) {
        return messageRepository.findByConversationIdOrderBySequenceAsc(conversationId);
    }
}

package com.criticalflow.domain.conversation.service;

import com.criticalflow.domain.conversation.entity.AiConversation;
import com.criticalflow.domain.conversation.entity.AiMessage;
import com.criticalflow.domain.conversation.entity.QuestionType;
import com.criticalflow.domain.conversation.repository.AiConversationRepository;
import com.criticalflow.domain.conversation.repository.AiMessageRepository;
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
    public List<AiConversation> getConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<AiMessage> getMessages(Long conversationId) {
        return messageRepository.findByConversationIdOrderBySequenceAsc(conversationId);
    }
}

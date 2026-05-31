package com.criticalflow.domain.conversation.service;

import com.criticalflow.domain.conversation.entity.AiConversation;
import com.criticalflow.domain.conversation.entity.AiMessage;
import com.criticalflow.domain.conversation.entity.QuestionType;
import com.criticalflow.domain.conversation.repository.AiConversationRepository;
import com.criticalflow.domain.conversation.repository.AiMessageRepository;
import com.criticalflow.global.exception.DomainException;
import com.criticalflow.global.exception.ErrorCode;
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

    @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        AiConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new DomainException(ErrorCode.CONVERSATION_NOT_FOUND));
        if (!conversation.getUserId().equals(userId)) {
            throw new DomainException(ErrorCode.CONVERSATION_NOT_FOUND);
        }
        messageRepository.deleteByConversationId(conversationId);
        conversationRepository.delete(conversation);
    }

    @Transactional(readOnly = true)
    public List<Long> getConversationIds(Long userId) {
        return conversationRepository.findConversationIdsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<AiMessage> getMessages(Long conversationId) {
        return messageRepository.findByConversationIdOrderBySequenceAsc(conversationId);
    }
}

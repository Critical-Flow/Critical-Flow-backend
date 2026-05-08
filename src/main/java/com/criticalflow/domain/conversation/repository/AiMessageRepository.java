package com.criticalflow.domain.conversation.repository;

import com.criticalflow.domain.conversation.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {

    List<AiMessage> findByConversationIdOrderBySequenceAsc(Long conversationId);

    long countByConversationIdAndRole(Long conversationId, AiMessage.MessageRole role);
}

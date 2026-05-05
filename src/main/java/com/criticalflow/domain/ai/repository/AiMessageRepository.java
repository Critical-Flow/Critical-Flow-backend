package com.criticalflow.domain.ai.repository;

import com.criticalflow.domain.ai.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {

    List<AiMessage> findByConversationIdOrderBySequenceAsc(Long conversationId);

    long countByConversationIdAndRole(Long conversationId, AiMessage.MessageRole role);
}

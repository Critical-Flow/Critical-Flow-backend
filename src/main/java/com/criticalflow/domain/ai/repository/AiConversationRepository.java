package com.criticalflow.domain.ai.repository;

import com.criticalflow.domain.ai.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {
}

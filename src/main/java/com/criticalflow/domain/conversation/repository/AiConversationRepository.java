package com.criticalflow.domain.conversation.repository;

import com.criticalflow.domain.conversation.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

    @Query("SELECT n.content, c.questionType FROM AiConversation c " +
           "JOIN StudyNote n ON c.noteId = n.noteId " +
           "WHERE c.questionType IS NOT NULL")
    List<Object[]> findTrainingData();

    @Query("SELECT c.conversationId FROM AiConversation c WHERE c.userId = :userId")
    List<Long> findConversationIdsByUserId(@Param("userId") Long userId);

    void deleteByUserId(Long userId);
}

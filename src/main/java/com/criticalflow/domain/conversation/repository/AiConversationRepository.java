package com.criticalflow.domain.conversation.repository;

import com.criticalflow.domain.conversation.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

    @Query("SELECT n.content, c.questionType FROM AiConversation c " +
           "JOIN StudyNote n ON c.noteId = n.noteId " +
           "WHERE c.questionType IS NOT NULL")
    List<Object[]> findTrainingData();

    List<Long> findConversationIdsByUserId(Long userId);

    void deleteByUserId(Long userId);
}

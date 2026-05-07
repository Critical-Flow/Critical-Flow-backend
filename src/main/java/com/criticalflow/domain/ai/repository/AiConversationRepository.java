package com.criticalflow.domain.ai.repository;

import com.criticalflow.domain.ai.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

    // Fine-tuning 데이터 추출: (노트 content, questionType) 쌍 반환
    @Query("SELECT n.content, c.questionType FROM AiConversation c " +
           "JOIN StudyNote n ON c.noteId = n.noteId " +
           "WHERE c.questionType IS NOT NULL")
    List<Object[]> findTrainingData();
}

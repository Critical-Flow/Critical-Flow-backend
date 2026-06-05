package com.criticalflow.domain.note.repository;

import com.criticalflow.domain.note.entity.StudyNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StudyNoteRepository extends JpaRepository<StudyNote, Long> {

    List<StudyNote> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<StudyNote> findBySessionId(Long sessionId);

    Optional<StudyNote> findByNoteIdAndUserId(Long noteId, Long userId);

    List<StudyNote> findByCategoryIdAndUserIdOrderByCreatedAtDesc(Long categoryId, Long userId);

    void deleteByUserId(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE StudyNote n SET n.categoryId = NULL WHERE n.categoryId = :categoryId")
    void clearCategoryId(Long categoryId);
}
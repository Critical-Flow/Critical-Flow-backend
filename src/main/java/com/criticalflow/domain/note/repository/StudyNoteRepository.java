package com.criticalflow.domain.note.repository;

import com.criticalflow.domain.note.entity.StudyNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyNoteRepository extends JpaRepository<StudyNote, Long> {

    List<StudyNote> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<StudyNote> findBySessionId(Long sessionId);

    Optional<StudyNote> findByNoteIdAndUserId(Long noteId, Long userId);
}
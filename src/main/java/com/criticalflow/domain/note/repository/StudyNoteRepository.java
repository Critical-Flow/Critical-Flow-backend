package com.criticalflow.domain.note.repository;

import com.criticalflow.domain.note.entity.StudyNote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyNoteRepository extends JpaRepository<StudyNote, Long> {
}
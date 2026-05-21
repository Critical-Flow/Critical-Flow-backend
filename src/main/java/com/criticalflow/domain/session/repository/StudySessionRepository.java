package com.criticalflow.domain.session.repository;

import com.criticalflow.domain.session.entity.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    Optional<StudySession> findBySessionIdAndUserId(Long sessionId, Long userId);

    List<StudySession> findByUserIdOrderByStartTimeDesc(Long userId);
}

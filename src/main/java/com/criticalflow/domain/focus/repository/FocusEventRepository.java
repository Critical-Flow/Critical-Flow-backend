package com.criticalflow.domain.focus.repository;

import com.criticalflow.domain.focus.entity.FocusEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FocusEventRepository extends JpaRepository<FocusEvent, Long> {

    List<FocusEvent> findBySessionIdAndDetectedAtAfterOrderByDetectedAtAsc(
            Long sessionId, LocalDateTime since);

    @Query("SELECT COALESCE(SUM(e.durationSec), 0) FROM FocusEvent e WHERE e.sessionId = :sessionId")
    int sumDurationSecBySessionId(@Param("sessionId") Long sessionId);
}
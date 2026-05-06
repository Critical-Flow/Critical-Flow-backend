package com.criticalflow.domain.focus.repository;

import com.criticalflow.domain.focus.entity.FocusEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FocusEventRepository extends JpaRepository<FocusEvent, Long> {

    List<FocusEvent> findBySessionIdAndDetectedAtAfterOrderByDetectedAtAsc(
            Long sessionId, LocalDateTime since);
}
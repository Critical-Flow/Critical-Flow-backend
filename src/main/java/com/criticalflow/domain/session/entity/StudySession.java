package com.criticalflow.domain.session.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_session")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class StudySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "total_study_minutes")
    private Integer totalStudyMinutes;

    @Column(name = "total_focus_minutes")
    private Integer totalFocusMinutes;

    public void end(LocalDateTime endTime, int totalStudyMinutes, int totalFocusMinutes) {
        this.endTime = endTime;
        this.totalStudyMinutes = totalStudyMinutes;
        this.totalFocusMinutes = totalFocusMinutes;
    }
}

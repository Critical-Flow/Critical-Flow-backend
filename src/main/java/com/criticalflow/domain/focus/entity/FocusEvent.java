package com.criticalflow.domain.focus.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "focus_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FocusEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(name = "duration_sec", nullable = false)
    private Integer durationSec;

    @Column(nullable = false)
    private Boolean alerted;

    public enum EventType {
        GAZE_OUT, DROWSY, ABSENT
    }
}
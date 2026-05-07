package com.criticalflow.domain.note.dto;

import com.criticalflow.domain.note.entity.StudyNote;

import java.time.LocalDateTime;

public record NoteResponse(
        Long noteId,
        Long userId,
        Long sessionId,
        Long categoryId,
        String title,
        String content,
        Boolean isSaved,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NoteResponse from(StudyNote note) {
        return new NoteResponse(
                note.getNoteId(),
                note.getUserId(),
                note.getSessionId(),
                note.getCategoryId(),
                note.getTitle(),
                note.getContent(),
                note.getIsSaved(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}

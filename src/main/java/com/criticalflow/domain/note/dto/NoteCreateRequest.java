package com.criticalflow.domain.note.dto;

public record NoteCreateRequest(
        Long sessionId,
        Long categoryId,
        String title,
        String content
) {}

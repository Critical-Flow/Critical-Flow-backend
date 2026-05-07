package com.criticalflow.domain.note.dto;

public record NoteUpdateRequest(
        String title,
        String content,
        Long categoryId
) {}

package com.criticalflow.domain.note.service;

import com.criticalflow.domain.note.dto.NoteCreateRequest;
import com.criticalflow.domain.note.dto.NoteResponse;
import com.criticalflow.domain.note.dto.NoteUpdateRequest;
import com.criticalflow.domain.note.entity.StudyNote;
import com.criticalflow.domain.note.repository.StudyNoteRepository;
import com.criticalflow.global.ai.rag.NoteEmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final StudyNoteRepository noteRepository;
    private final NoteEmbeddingService noteEmbeddingService;

    @Transactional
    public NoteResponse saveNote(Long userId, NoteCreateRequest request) {
        StudyNote note = StudyNote.builder()
                .userId(userId)
                .sessionId(request.sessionId())
                .categoryId(request.categoryId())
                .title(request.title())
                .content(request.content())
                .isSaved(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        StudyNote saved = noteRepository.save(note);
        noteEmbeddingService.embed(saved);
        return NoteResponse.from(saved);
    }

    @Transactional
    public NoteResponse updateNote(Long userId, Long noteId, NoteUpdateRequest request) {
        StudyNote note = noteRepository.findByNoteIdAndUserId(noteId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Note not found: " + noteId));

        note.update(request.title(), request.content(), request.categoryId());
        noteEmbeddingService.embed(note);
        return NoteResponse.from(note);
    }

    @Transactional
    public void deleteNote(Long userId, Long noteId) {
        StudyNote note = noteRepository.findByNoteIdAndUserId(noteId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Note not found: " + noteId));

        noteRepository.delete(note);
        noteEmbeddingService.delete(note.getNoteId());
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> getNotesByUser(Long userId) {
        return noteRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NoteResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public NoteResponse getNote(Long userId, Long noteId) {
        StudyNote note = noteRepository.findByNoteIdAndUserId(noteId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Note not found: " + noteId));

        return NoteResponse.from(note);
    }
}

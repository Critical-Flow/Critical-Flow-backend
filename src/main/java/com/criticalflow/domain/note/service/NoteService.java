package com.criticalflow.domain.note.service;

import com.criticalflow.domain.note.dto.NoteCreateRequest;
import com.criticalflow.domain.note.dto.NoteResponse;
import com.criticalflow.domain.note.dto.NoteUpdateRequest;
import com.criticalflow.domain.note.entity.StudyNote;
import com.criticalflow.domain.note.repository.StudyNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final StudyNoteRepository noteRepository;

    @Transactional
    public NoteResponse saveNote(Long userId, NoteCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();
        StudyNote note = StudyNote.builder()
                .userId(userId)
                .sessionId(request.sessionId())
                .categoryId(request.categoryId())
                .title(request.title())
                .content(request.content())
                .isSaved(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return NoteResponse.from(noteRepository.save(note));
    }

    @Transactional
    public NoteResponse updateNote(Long userId, Long noteId, NoteUpdateRequest request) {
        StudyNote note = noteRepository.findByNoteIdAndUserId(noteId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Note not found: " + noteId));

        note.update(request.title(), request.content(), request.categoryId());
        return NoteResponse.from(note);
    }

    @Transactional
    public void deleteNote(Long userId, Long noteId) {
        StudyNote note = noteRepository.findByNoteIdAndUserId(noteId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Note not found: " + noteId));

        noteRepository.delete(note);
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

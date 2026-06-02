package com.criticalflow.domain.note.service;

import com.criticalflow.domain.note.dto.NoteCreateRequest;
import com.criticalflow.domain.note.dto.NoteResponse;
import com.criticalflow.domain.note.dto.NoteUpdateRequest;
import com.criticalflow.domain.note.entity.StudyNote;
import com.criticalflow.domain.note.repository.StudyNoteRepository;
import com.criticalflow.global.ai.rag.NoteEmbeddingService;
import com.criticalflow.global.exception.DomainException;
import com.criticalflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoteService {

    private static final int MAX_NOTE_LENGTH = 2_000;

    private final StudyNoteRepository noteRepository;
    private final NoteEmbeddingService noteEmbeddingService;

    @Transactional
    public NoteResponse saveNote(Long userId, NoteCreateRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new DomainException(ErrorCode.NOTE_TITLE_REQUIRED);
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new DomainException(ErrorCode.NOTE_CONTENT_REQUIRED);
        }
        if (request.content().length() > MAX_NOTE_LENGTH) {
            throw new DomainException(ErrorCode.NOTE_CONTENT_TOO_LONG);
        }

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

        StudyNote saved = noteRepository.save(note);
        noteEmbeddingService.embed(saved);
        return NoteResponse.from(saved);
    }

    @Transactional
    public NoteResponse updateNote(Long userId, Long noteId, NoteUpdateRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new DomainException(ErrorCode.NOTE_TITLE_REQUIRED);
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new DomainException(ErrorCode.NOTE_CONTENT_REQUIRED);
        }
        if (request.content().length() > MAX_NOTE_LENGTH) {
            throw new DomainException(ErrorCode.NOTE_CONTENT_TOO_LONG);
        }

        StudyNote note = getOwnedNote(userId, noteId);

        note.update(request.title(), request.content(), request.categoryId());
        noteEmbeddingService.embed(note);
        return NoteResponse.from(note);
    }

    @Transactional
    public void deleteNote(Long userId, Long noteId) {
        StudyNote note = getOwnedNote(userId, noteId);

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
        StudyNote note = getOwnedNote(userId, noteId);

        return NoteResponse.from(note);
    }

    @Transactional(readOnly = true)
    public int reembedAll() {
        List<StudyNote> all = noteRepository.findAll();
        all.forEach(noteEmbeddingService::embed);
        return all.size();
    }

    private StudyNote getOwnedNote(Long userId, Long noteId) {
        StudyNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOTE_NOT_FOUND));

        if (!note.getUserId().equals(userId)) {
            throw new DomainException(ErrorCode.NOTE_ACCESS_DENIED);
        }

        return note;
    }
}

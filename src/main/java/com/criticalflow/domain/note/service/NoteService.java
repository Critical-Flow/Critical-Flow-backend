package com.criticalflow.domain.note.service;

import com.criticalflow.domain.note.dto.NoteCreateRequest;
import com.criticalflow.domain.note.dto.NoteResponse;
import com.criticalflow.domain.note.dto.NoteUpdateRequest;
import com.criticalflow.domain.note.entity.StudyNote;
import com.criticalflow.domain.note.repository.StudyNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        StudyNote note = getOwnedNote(userId, noteId);

        note.update(request.title(), request.content(), request.categoryId());
        return NoteResponse.from(note);
    }

    @Transactional
    public void deleteNote(Long userId, Long noteId) {
        StudyNote note = getOwnedNote(userId, noteId);

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
        StudyNote note = getOwnedNote(userId, noteId);

        return NoteResponse.from(note);
    }

    private StudyNote getOwnedNote(Long userId, Long noteId) {
        StudyNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found"));

        if (!note.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return note;
    }
}

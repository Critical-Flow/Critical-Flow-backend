package com.criticalflow.domain.note.controller;

import com.criticalflow.domain.note.dto.NoteCreateRequest;
import com.criticalflow.domain.note.dto.NoteResponse;
import com.criticalflow.domain.note.dto.NoteUpdateRequest;
import com.criticalflow.domain.note.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @PostMapping
    public ResponseEntity<NoteResponse> create(
            @RequestParam Long userId,
            @RequestBody NoteCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noteService.saveNote(userId, request));
    }

    @PutMapping("/{noteId}")
    public ResponseEntity<NoteResponse> update(
            @RequestParam Long userId,
            @PathVariable Long noteId,
            @RequestBody NoteUpdateRequest request) {
        return ResponseEntity.ok(noteService.updateNote(userId, noteId, request));
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> delete(
            @RequestParam Long userId,
            @PathVariable Long noteId) {
        noteService.deleteNote(userId, noteId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<NoteResponse>> getAll(@RequestParam Long userId) {
        return ResponseEntity.ok(noteService.getNotesByUser(userId));
    }

    @GetMapping("/{noteId}")
    public ResponseEntity<NoteResponse> getOne(
            @RequestParam Long userId,
            @PathVariable Long noteId) {
        return ResponseEntity.ok(noteService.getNote(userId, noteId));
    }
}

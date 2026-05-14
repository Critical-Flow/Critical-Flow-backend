package com.criticalflow.domain.note.controller;

import com.criticalflow.domain.note.dto.NoteCreateRequest;
import com.criticalflow.domain.note.dto.NoteResponse;
import com.criticalflow.domain.note.dto.NoteUpdateRequest;
import com.criticalflow.domain.note.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Note", description = "학습 노트 CRUD 및 재임베딩 API")
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @Operation(summary = "노트 생성", description = "사용자의 학습 세션에 새 노트를 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "노트 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<NoteResponse> create(
            @Parameter(description = "사용자 ID", required = true) @RequestParam Long userId,
            @RequestBody NoteCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noteService.saveNote(userId, request));
    }

    @Operation(summary = "노트 수정", description = "제목, 내용, 카테고리를 수정합니다. 전달한 필드만 반영됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "노트 수정 성공"),
            @ApiResponse(responseCode = "403", description = "다른 사용자의 노트에 접근"),
            @ApiResponse(responseCode = "404", description = "노트를 찾을 수 없음")
    })
    @PutMapping("/{noteId}")
    public ResponseEntity<NoteResponse> update(
            @Parameter(description = "사용자 ID", required = true) @RequestParam Long userId,
            @Parameter(description = "수정할 노트 ID", required = true) @PathVariable Long noteId,
            @RequestBody NoteUpdateRequest request) {
        return ResponseEntity.ok(noteService.updateNote(userId, noteId, request));
    }

    @Operation(summary = "노트 삭제", description = "노트를 삭제합니다. 본인 소유 노트만 삭제할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "노트 삭제 성공"),
            @ApiResponse(responseCode = "403", description = "다른 사용자의 노트에 접근"),
            @ApiResponse(responseCode = "404", description = "노트를 찾을 수 없음")
    })
    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "사용자 ID", required = true) @RequestParam Long userId,
            @Parameter(description = "삭제할 노트 ID", required = true) @PathVariable Long noteId) {
        noteService.deleteNote(userId, noteId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "노트 목록 조회", description = "사용자의 전체 노트 목록을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping
    public ResponseEntity<List<NoteResponse>> getAll(
            @Parameter(description = "사용자 ID", required = true) @RequestParam Long userId) {
        return ResponseEntity.ok(noteService.getNotesByUser(userId));
    }

    @Operation(summary = "노트 단건 조회", description = "노트 ID로 특정 노트를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "다른 사용자의 노트에 접근"),
            @ApiResponse(responseCode = "404", description = "노트를 찾을 수 없음")
    })
    @GetMapping("/{noteId}")
    public ResponseEntity<NoteResponse> getOne(
            @Parameter(description = "사용자 ID", required = true) @RequestParam Long userId,
            @Parameter(description = "조회할 노트 ID", required = true) @PathVariable Long noteId) {
        return ResponseEntity.ok(noteService.getNote(userId, noteId));
    }

    @Operation(
            summary = "전체 노트 재임베딩",
            description = "저장된 모든 노트를 ChromaDB에 재임베딩합니다. 임베딩 모델 변경 또는 데이터 초기화 후 사용합니다."
    )
    @ApiResponse(responseCode = "200", description = "재임베딩 완료, 처리된 노트 수 반환")
    @PostMapping("/reembed")
    public ResponseEntity<String> reembedAll() {
        int count = noteService.reembedAll();
        return ResponseEntity.ok("재임베딩 완료: " + count + "개");
    }
}

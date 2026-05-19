package com.criticalflow.domain.category.controller;

import com.criticalflow.domain.category.dto.CategoryCreateRequest;
import com.criticalflow.domain.category.dto.CategoryResponse;
import com.criticalflow.domain.category.dto.CategoryUpdateRequest;
import com.criticalflow.domain.category.service.CategoryService;
import com.criticalflow.domain.note.dto.NoteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Category", description = "카테고리(디렉터리) CRUD 및 노트 조회 API")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(
            summary = "카테고리 생성",
            description = "새 카테고리를 생성합니다. title은 필수, description은 선택입니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "카테고리 생성 성공"),
            @ApiResponse(responseCode = "400", description = "필수 필드(title) 누락"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료")
    })
    @PostMapping
    public ResponseEntity<CategoryResponse> create(
            @AuthenticationPrincipal Long userId,
            @RequestBody CategoryCreateRequest request
    ) {
        CategoryResponse response = categoryService.create(userId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/categories/" + response.categoryId()))
                .body(response);
    }

    @Operation(
            summary = "내 카테고리 목록 조회",
            description = "현재 로그인한 사용자의 카테고리 목록을 최신순으로 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료")
    })
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(categoryService.getCategories(userId));
    }

    @Operation(
            summary = "카테고리 수정",
            description = "카테고리의 title 또는 description을 수정합니다. null로 전달한 필드는 기존 값을 유지합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "404", description = "카테고리를 찾을 수 없음 (본인 소유 아닌 경우 포함)")
    })
    @PatchMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> update(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "수정할 카테고리 ID", required = true) @PathVariable Long categoryId,
            @RequestBody CategoryUpdateRequest request
    ) {
        return ResponseEntity.ok(categoryService.update(userId, categoryId, request));
    }

    @Operation(
            summary = "카테고리 내 노트 목록 조회",
            description = "특정 카테고리에 속한 노트 목록을 최신순으로 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "404", description = "카테고리를 찾을 수 없음 (본인 소유 아닌 경우 포함)")
    })
    @GetMapping("/{categoryId}/notes")
    public ResponseEntity<List<NoteResponse>> getNotes(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "조회할 카테고리 ID", required = true) @PathVariable Long categoryId
    ) {
        return ResponseEntity.ok(categoryService.getNotesByCategory(userId, categoryId));
    }

    @Operation(
            summary = "카테고리 삭제",
            description = "카테고리를 삭제합니다. 속한 노트의 카테고리 연결은 null로 해제됩니다 (노트는 유지).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "404", description = "카테고리를 찾을 수 없음 (본인 소유 아닌 경우 포함)")
    })
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "삭제할 카테고리 ID", required = true) @PathVariable Long categoryId
    ) {
        categoryService.delete(userId, categoryId);
        return ResponseEntity.noContent().build();
    }
}

package com.criticalflow.domain.category.controller;

import com.criticalflow.domain.category.dto.CategoryCreateRequest;
import com.criticalflow.domain.category.dto.CategoryResponse;
import com.criticalflow.domain.category.dto.CategoryUpdateRequest;
import com.criticalflow.domain.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

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

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(categoryService.getCategories(userId));
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long categoryId,
            @RequestBody CategoryUpdateRequest request
    ) {
        return ResponseEntity.ok(categoryService.update(userId, categoryId, request));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long categoryId
    ) {
        categoryService.delete(userId, categoryId);
        return ResponseEntity.noContent().build();
    }
}

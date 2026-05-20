package com.criticalflow.domain.category.dto;

import com.criticalflow.domain.category.entity.Category;

import java.time.LocalDateTime;

public record CategoryResponse(
        Long categoryId,
        String title,
        String description,
        LocalDateTime createdAt
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getCategoryId(),
                category.getTitle(),
                category.getDescription(),
                category.getCreatedAt()
        );
    }
}

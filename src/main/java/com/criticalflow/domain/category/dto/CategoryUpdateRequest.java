package com.criticalflow.domain.category.dto;

public record CategoryUpdateRequest(
        String title,
        String description
) {
}

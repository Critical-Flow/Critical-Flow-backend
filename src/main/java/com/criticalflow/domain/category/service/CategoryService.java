package com.criticalflow.domain.category.service;

import com.criticalflow.domain.category.dto.CategoryCreateRequest;
import com.criticalflow.domain.category.dto.CategoryResponse;
import com.criticalflow.domain.category.dto.CategoryUpdateRequest;
import com.criticalflow.domain.category.entity.Category;
import com.criticalflow.domain.category.repository.CategoryRepository;
import com.criticalflow.global.exception.DomainException;
import com.criticalflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse create(Long userId, CategoryCreateRequest request) {
        Category category = Category.builder()
                .userId(userId)
                .title(request.title())
                .description(request.description())
                .createdAt(LocalDateTime.now())
                .build();
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(Long userId) {
        return categoryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public CategoryResponse update(Long userId, Long categoryId, CategoryUpdateRequest request) {
        Category category = findOwned(userId, categoryId);
        category.update(request.title(), request.description());
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long userId, Long categoryId) {
        Category category = findOwned(userId, categoryId);
        categoryRepository.delete(category);
    }

    private Category findOwned(Long userId, Long categoryId) {
        return categoryRepository.findByCategoryIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new DomainException(ErrorCode.CATEGORY_NOT_FOUND));
    }
}

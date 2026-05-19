package com.criticalflow.domain.category.repository;

import com.criticalflow.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Category> findByCategoryIdAndUserId(Long categoryId, Long userId);
}

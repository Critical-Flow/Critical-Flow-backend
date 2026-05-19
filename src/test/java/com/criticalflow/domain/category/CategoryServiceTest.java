package com.criticalflow.domain.category;

import com.criticalflow.domain.category.dto.CategoryCreateRequest;
import com.criticalflow.domain.category.dto.CategoryResponse;
import com.criticalflow.domain.category.dto.CategoryUpdateRequest;
import com.criticalflow.domain.category.entity.Category;
import com.criticalflow.domain.category.repository.CategoryRepository;
import com.criticalflow.domain.category.service.CategoryService;
import com.criticalflow.domain.note.dto.NoteResponse;
import com.criticalflow.domain.note.entity.StudyNote;
import com.criticalflow.domain.note.repository.StudyNoteRepository;
import com.criticalflow.global.exception.DomainException;
import com.criticalflow.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private StudyNoteRepository studyNoteRepository;

    @InjectMocks
    private CategoryService categoryService;

    private static final Long USER_ID = 1L;
    private static final Long CATEGORY_ID = 10L;

    private Category sampleCategory() {
        return Category.builder()
                .categoryId(CATEGORY_ID)
                .userId(USER_ID)
                .title("알고리즘")
                .description("알고리즘 공부 카테고리")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private StudyNote sampleNote() {
        return StudyNote.builder()
                .noteId(1L)
                .userId(USER_ID)
                .categoryId(CATEGORY_ID)
                .sessionId(1L)
                .title("DFS 정리")
                .content("내용")
                .isSaved(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("카테고리 생성")
    class Create {

        @Test
        @DisplayName("정상적으로 카테고리를 생성하고 응답을 반환한다")
        void 카테고리_생성_성공() {
            CategoryCreateRequest request = new CategoryCreateRequest("알고리즘", "알고리즘 공부 카테고리");
            Category saved = sampleCategory();
            when(categoryRepository.save(any(Category.class))).thenReturn(saved);

            CategoryResponse response = categoryService.create(USER_ID, request);

            assertThat(response.categoryId()).isEqualTo(CATEGORY_ID);
            assertThat(response.title()).isEqualTo("알고리즘");
            assertThat(response.description()).isEqualTo("알고리즘 공부 카테고리");
            verify(categoryRepository).save(any(Category.class));
        }
    }

    @Nested
    @DisplayName("카테고리 목록 조회")
    class GetCategories {

        @Test
        @DisplayName("유저의 카테고리 목록을 최신순으로 반환한다")
        void 카테고리_목록_조회_성공() {
            List<Category> categories = List.of(sampleCategory());
            when(categoryRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(categories);

            List<CategoryResponse> result = categoryService.getCategories(USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).categoryId()).isEqualTo(CATEGORY_ID);
        }
    }

    @Nested
    @DisplayName("카테고리 수정")
    class Update {

        @Test
        @DisplayName("정상적으로 카테고리를 수정한다")
        void 카테고리_수정_성공() {
            CategoryUpdateRequest request = new CategoryUpdateRequest("자료구조", "자료구조 정리");
            when(categoryRepository.findByCategoryIdAndUserId(CATEGORY_ID, USER_ID))
                    .thenReturn(Optional.of(sampleCategory()));

            CategoryResponse response = categoryService.update(USER_ID, CATEGORY_ID, request);

            assertThat(response.title()).isEqualTo("자료구조");
            assertThat(response.description()).isEqualTo("자료구조 정리");
        }

        @Test
        @DisplayName("존재하지 않는 카테고리 수정 시 CATEGORY_NOT_FOUND 예외가 발생한다")
        void 존재하지_않는_카테고리_수정_예외() {
            when(categoryRepository.findByCategoryIdAndUserId(CATEGORY_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.update(USER_ID, CATEGORY_ID, new CategoryUpdateRequest("수정", null)))
                    .isInstanceOf(DomainException.class)
                    .extracting(e -> ((DomainException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
        }

        @Test
        @DisplayName("null 필드는 기존 값을 유지한다")
        void null_필드는_기존값_유지() {
            CategoryUpdateRequest request = new CategoryUpdateRequest(null, "새 설명");
            when(categoryRepository.findByCategoryIdAndUserId(CATEGORY_ID, USER_ID))
                    .thenReturn(Optional.of(sampleCategory()));

            CategoryResponse response = categoryService.update(USER_ID, CATEGORY_ID, request);

            assertThat(response.title()).isEqualTo("알고리즘");
            assertThat(response.description()).isEqualTo("새 설명");
        }
    }

    @Nested
    @DisplayName("카테고리 내 노트 조회")
    class GetNotesByCategory {

        @Test
        @DisplayName("카테고리에 속한 노트 목록을 반환한다")
        void 카테고리_노트_목록_반환() {
            when(categoryRepository.findByCategoryIdAndUserId(CATEGORY_ID, USER_ID))
                    .thenReturn(Optional.of(sampleCategory()));
            when(studyNoteRepository.findByCategoryIdAndUserIdOrderByCreatedAtDesc(CATEGORY_ID, USER_ID))
                    .thenReturn(List.of(sampleNote()));

            List<NoteResponse> result = categoryService.getNotesByCategory(USER_ID, CATEGORY_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).categoryId()).isEqualTo(CATEGORY_ID);
        }

        @Test
        @DisplayName("존재하지 않는 카테고리 조회 시 CATEGORY_NOT_FOUND 예외가 발생한다")
        void 존재하지_않는_카테고리_노트_조회_예외() {
            when(categoryRepository.findByCategoryIdAndUserId(CATEGORY_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.getNotesByCategory(USER_ID, CATEGORY_ID))
                    .isInstanceOf(DomainException.class)
                    .extracting(e -> ((DomainException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("카테고리 삭제")
    class Delete {

        @Test
        @DisplayName("카테고리 삭제 시 노트 연결 해제 후 카테고리를 삭제한다")
        void 카테고리_삭제_순서_검증() {
            Category category = sampleCategory();
            when(categoryRepository.findByCategoryIdAndUserId(CATEGORY_ID, USER_ID))
                    .thenReturn(Optional.of(category));

            categoryService.delete(USER_ID, CATEGORY_ID);

            var inOrder = inOrder(studyNoteRepository, categoryRepository);
            inOrder.verify(studyNoteRepository).clearCategoryId(CATEGORY_ID);
            inOrder.verify(categoryRepository).delete(category);
        }

        @Test
        @DisplayName("존재하지 않는 카테고리 삭제 시 CATEGORY_NOT_FOUND 예외가 발생한다")
        void 존재하지_않는_카테고리_삭제_예외() {
            when(categoryRepository.findByCategoryIdAndUserId(CATEGORY_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.delete(USER_ID, CATEGORY_ID))
                    .isInstanceOf(DomainException.class)
                    .extracting(e -> ((DomainException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);

            verify(studyNoteRepository, never()).clearCategoryId(any());
            verify(categoryRepository, never()).delete(any());
        }
    }
}

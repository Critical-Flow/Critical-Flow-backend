package com.criticalflow.domain.user;

import com.criticalflow.domain.auth.repository.RefreshTokenRepository;
import com.criticalflow.domain.conversation.repository.AiConversationRepository;
import com.criticalflow.domain.conversation.repository.AiMessageRepository;
import com.criticalflow.domain.note.repository.StudyNoteRepository;
import com.criticalflow.domain.user.dto.ProfileResponse;
import com.criticalflow.domain.user.dto.ProfileUpdateRequest;
import com.criticalflow.domain.user.dto.UserInfoResponse;
import com.criticalflow.domain.user.entity.User;
import com.criticalflow.domain.user.repository.UserRepository;
import com.criticalflow.domain.user.service.UserService;
import com.criticalflow.global.exception.DomainException;
import com.criticalflow.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private AiConversationRepository aiConversationRepository;
    @Mock private AiMessageRepository aiMessageRepository;
    @Mock private StudyNoteRepository studyNoteRepository;

    @InjectMocks
    private UserService userService;

    private static final Long USER_ID = 1L;

    private User stubUser() {
        return User.builder()
                .userId(USER_ID)
                .githubId(12345L)
                .name("테스터")
                .email("test@example.com")
                .affiliation("테스트 대학교")
                .createdAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .build();
    }

    @Nested
    @DisplayName("회원 정보 조회")
    class GetUserInfoTest {

        @Test
        @DisplayName("존재하는 userId로 요청 시 UserInfoResponse가 반환된다")
        void returnsUserInfo() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(stubUser()));

            UserInfoResponse result = userService.getUserInfo(USER_ID);

            assertThat(result.userId()).isEqualTo(USER_ID);
            assertThat(result.name()).isEqualTo("테스터");
            assertThat(result.email()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("존재하지 않는 userId로 요청 시 USER_NOT_FOUND 예외가 발생한다")
        void throwsWhenUserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserInfo(USER_ID))
                    .isInstanceOf(DomainException.class)
                    .satisfies(e -> assertThat(((DomainException) e).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("프로필 조회")
    class GetProfileTest {

        @Test
        @DisplayName("존재하는 userId로 요청 시 name과 affiliation이 담긴 ProfileResponse가 반환된다")
        void returnsProfile() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(stubUser()));

            ProfileResponse result = userService.getProfile(USER_ID);

            assertThat(result.name()).isEqualTo("테스터");
            assertThat(result.affiliation()).isEqualTo("테스트 대학교");
        }
    }

    @Nested
    @DisplayName("프로필 수정")
    class UpdateProfileTest {

        @Test
        @DisplayName("name과 affiliation 수정 후 변경값이 반영된 ProfileResponse가 반환된다")
        void returnsUpdatedProfile() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(stubUser()));

            ProfileResponse result = userService.updateProfile(USER_ID,
                    new ProfileUpdateRequest("새이름", "새소속"));

            assertThat(result.name()).isEqualTo("새이름");
            assertThat(result.affiliation()).isEqualTo("새소속");
        }

        @Test
        @DisplayName("존재하지 않는 userId로 요청 시 USER_NOT_FOUND 예외가 발생한다")
        void throwsWhenUserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateProfile(USER_ID,
                    new ProfileUpdateRequest("이름", "소속")))
                    .isInstanceOf(DomainException.class)
                    .satisfies(e -> assertThat(((DomainException) e).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("탈퇴")
    class WithdrawTest {

        @Test
        @DisplayName("존재하지 않는 userId로 요청 시 USER_NOT_FOUND 예외가 발생한다")
        void throwsWhenUserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.withdraw(USER_ID))
                    .isInstanceOf(DomainException.class)
                    .satisfies(e -> assertThat(((DomainException) e).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("AiMessage 삭제가 AiConversation 삭제보다 먼저 호출된다")
        void deletesMessagesBeforeConversations() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(stubUser()));
            when(aiConversationRepository.findConversationIdsByUserId(USER_ID))
                    .thenReturn(List.of(10L, 20L));

            userService.withdraw(USER_ID);

            InOrder order = inOrder(aiMessageRepository, aiConversationRepository);
            order.verify(aiMessageRepository).deleteByConversationIdIn(List.of(10L, 20L));
            order.verify(aiConversationRepository).deleteByUserId(USER_ID);
        }

        @Test
        @DisplayName("AiConversation 삭제가 StudyNote 삭제보다 먼저 호출된다")
        void deletesConversationsBeforeNotes() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(stubUser()));
            when(aiConversationRepository.findConversationIdsByUserId(USER_ID)).thenReturn(List.of());

            userService.withdraw(USER_ID);

            InOrder order = inOrder(aiConversationRepository, studyNoteRepository);
            order.verify(aiConversationRepository).deleteByUserId(USER_ID);
            order.verify(studyNoteRepository).deleteByUserId(USER_ID);
        }

        @Test
        @DisplayName("RefreshToken 삭제가 User 삭제보다 먼저 호출된다")
        void deletesRefreshTokenBeforeUser() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(stubUser()));
            when(aiConversationRepository.findConversationIdsByUserId(USER_ID)).thenReturn(List.of());

            userService.withdraw(USER_ID);

            InOrder order = inOrder(refreshTokenRepository, userRepository);
            order.verify(refreshTokenRepository).deleteByUserId(USER_ID);
            order.verify(userRepository).deleteById(USER_ID);
        }

        @Test
        @DisplayName("conversationIds가 비어있으면 deleteByConversationIdIn이 호출되지 않는다")
        void skipsMessageDeletionWhenNoConversations() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(stubUser()));
            when(aiConversationRepository.findConversationIdsByUserId(USER_ID)).thenReturn(List.of());

            userService.withdraw(USER_ID);

            verify(aiMessageRepository, never()).deleteByConversationIdIn(anyList());
        }
    }
}

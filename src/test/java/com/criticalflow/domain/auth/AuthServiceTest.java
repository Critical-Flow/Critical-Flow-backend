package com.criticalflow.domain.auth;

import com.criticalflow.domain.auth.entity.RefreshToken;
import com.criticalflow.domain.auth.repository.RefreshTokenRepository;
import com.criticalflow.domain.auth.service.AuthService;
import com.criticalflow.global.auth.jwt.JwtProvider;
import com.criticalflow.global.exception.DomainException;
import com.criticalflow.global.exception.ErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtProvider jwtProvider;
    @Mock private HttpServletResponse response;

    @InjectMocks
    private AuthService authService;

    private static final Long USER_ID = 1L;
    private static final String TOKEN_VALUE = "refresh.token.value";
    private static final String NEW_ACCESS_TOKEN = "new.access.token";

    private RefreshToken validToken() {
        return RefreshToken.builder()
                .userId(USER_ID)
                .tokenValue(TOKEN_VALUE)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
    }

    private RefreshToken expiredToken() {
        return RefreshToken.builder()
                .userId(USER_ID)
                .tokenValue(TOKEN_VALUE)
                .expiresAt(LocalDateTime.now().minusSeconds(1))
                .build();
    }

    @Nested
    @DisplayName("로그아웃")
    class LogoutTest {

        @Test
        @DisplayName("userId에 해당하는 RefreshToken이 DB에서 삭제된다")
        void deletesRefreshToken() {
            authService.logout(USER_ID, response);

            verify(refreshTokenRepository).deleteByUserId(USER_ID);
        }

        @Test
        @DisplayName("응답 쿠키의 refresh_token이 maxAge=0으로 설정된다")
        void expiresCookie() {
            authService.logout(USER_ID, response);

            ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
            verify(response).addCookie(captor.capture());
            Cookie cookie = captor.getValue();
            assertThat(cookie.getName()).isEqualTo("refresh_token");
            assertThat(cookie.getMaxAge()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("AccessToken 재발급")
    class ReissueTest {

        @Test
        @DisplayName("유효한 RefreshToken으로 요청 시 새 AccessToken이 반환된다")
        void returnsNewAccessToken() {
            when(refreshTokenRepository.findByTokenValue(TOKEN_VALUE)).thenReturn(Optional.of(validToken()));
            when(jwtProvider.generateToken(USER_ID)).thenReturn(NEW_ACCESS_TOKEN);

            String result = authService.reissue(TOKEN_VALUE);

            assertThat(result).isEqualTo(NEW_ACCESS_TOKEN);
        }

        @Test
        @DisplayName("DB에 없는 tokenValue로 요청 시 INVALID_REFRESH_TOKEN 예외가 발생한다")
        void throwsWhenTokenNotFound() {
            when(refreshTokenRepository.findByTokenValue(TOKEN_VALUE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.reissue(TOKEN_VALUE))
                    .isInstanceOf(DomainException.class)
                    .satisfies(e -> assertThat(((DomainException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
        }

        @Test
        @DisplayName("만료된 RefreshToken으로 요청 시 EXPIRED_REFRESH_TOKEN 예외가 발생한다")
        void throwsWhenTokenExpired() {
            when(refreshTokenRepository.findByTokenValue(TOKEN_VALUE)).thenReturn(Optional.of(expiredToken()));

            assertThatThrownBy(() -> authService.reissue(TOKEN_VALUE))
                    .isInstanceOf(DomainException.class)
                    .satisfies(e -> assertThat(((DomainException) e).getErrorCode())
                            .isEqualTo(ErrorCode.EXPIRED_REFRESH_TOKEN));
        }

        @Test
        @DisplayName("만료된 RefreshToken은 예외 발생 전에 DB에서 삭제된다")
        void deletesExpiredTokenBeforeThrowing() {
            RefreshToken expired = expiredToken();
            when(refreshTokenRepository.findByTokenValue(TOKEN_VALUE)).thenReturn(Optional.of(expired));

            assertThatThrownBy(() -> authService.reissue(TOKEN_VALUE))
                    .isInstanceOf(DomainException.class);

            verify(refreshTokenRepository).delete(expired);
            verify(jwtProvider, never()).generateToken(any());
        }
    }
}

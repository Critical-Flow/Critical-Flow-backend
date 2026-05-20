package com.criticalflow.global.auth;

import com.criticalflow.domain.auth.entity.RefreshToken;
import com.criticalflow.domain.auth.repository.RefreshTokenRepository;
import com.criticalflow.domain.user.entity.User;
import com.criticalflow.domain.user.repository.UserRepository;
import com.criticalflow.global.auth.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.RedirectStrategy;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock private JwtProvider jwtProvider;
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private Authentication authentication;
    @Mock private OAuth2User oAuth2User;
    @Mock private RedirectStrategy redirectStrategy;

    private OAuth2SuccessHandler handler;

    private static final Long GITHUB_ID = 12345L;
    private static final Long USER_ID = 1L;
    private static final String ACCESS_TOKEN = "access.token.value";
    private static final String REFRESH_TOKEN_VALUE = "refresh.token.value";
    private static final String CALLBACK_URL = "http://localhost:5173/oauth/callback";

    @BeforeEach
    void setUp() {
        handler = new OAuth2SuccessHandler(jwtProvider, userRepository, refreshTokenRepository, CALLBACK_URL);
        handler.setRedirectStrategy(redirectStrategy);
    }

    private User stubUser() {
        return User.builder()
                .userId(USER_ID)
                .githubId(GITHUB_ID)
                .name("테스터")
                .email("test@example.com")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void stubOAuth2(User user) {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("id")).thenReturn(GITHUB_ID.intValue());
        when(userRepository.findByGithubId(GITHUB_ID)).thenReturn(Optional.of(user));
        when(jwtProvider.generateToken(USER_ID)).thenReturn(ACCESS_TOKEN);
        when(jwtProvider.generateRefreshToken(USER_ID)).thenReturn(REFRESH_TOKEN_VALUE);
        when(jwtProvider.getRefreshExpiration()).thenReturn(1209600000L);
    }

    @Nested
    @DisplayName("로그인 성공 정상 흐름")
    class SuccessFlowTest {

        @Test
        @DisplayName("AccessToken이 리다이렉트 URL의 token 쿼리 파라미터에 포함된다")
        void redirectsWithAccessToken() throws IOException {
            stubOAuth2(stubUser());

            handler.onAuthenticationSuccess(request, response, authentication);

            verify(redirectStrategy).sendRedirect(eq(request), eq(response),
                    contains("?token=" + ACCESS_TOKEN));
        }

        @Test
        @DisplayName("RefreshToken이 DB에 저장된다")
        void savesRefreshTokenToDb() throws IOException {
            stubOAuth2(stubUser());

            handler.onAuthenticationSuccess(request, response, authentication);

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
            assertThat(captor.getValue().getTokenValue()).isEqualTo(REFRESH_TOKEN_VALUE);
        }

        @Test
        @DisplayName("RefreshToken이 HttpOnly 쿠키로 응답에 설정된다")
        void setsHttpOnlyCookie() throws IOException {
            stubOAuth2(stubUser());

            handler.onAuthenticationSuccess(request, response, authentication);

            ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
            verify(response).addCookie(captor.capture());
            Cookie cookie = captor.getValue();
            assertThat(cookie.getName()).isEqualTo("refresh_token");
            assertThat(cookie.getValue()).isEqualTo(REFRESH_TOKEN_VALUE);
            assertThat(cookie.isHttpOnly()).isTrue();
        }

        @Test
        @DisplayName("기존 RefreshToken 삭제 후 새 토큰이 저장된다")
        void deletesBeforeSave() throws IOException {
            stubOAuth2(stubUser());

            handler.onAuthenticationSuccess(request, response, authentication);

            InOrder order = inOrder(refreshTokenRepository);
            order.verify(refreshTokenRepository).deleteByUserId(USER_ID);
            order.verify(refreshTokenRepository).save(any(RefreshToken.class));
        }
    }

    @Nested
    @DisplayName("예외 흐름")
    class ExceptionFlowTest {

        @Test
        @DisplayName("githubId에 해당하는 User가 없으면 IllegalStateException이 발생한다")
        void throwsWhenUserNotFound() {
            when(authentication.getPrincipal()).thenReturn(oAuth2User);
            when(oAuth2User.getAttribute("id")).thenReturn(GITHUB_ID.intValue());
            when(userRepository.findByGithubId(GITHUB_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    handler.onAuthenticationSuccess(request, response, authentication))
                    .isInstanceOf(IllegalStateException.class);

            verify(refreshTokenRepository, never()).save(any());
        }
    }
}

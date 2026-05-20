package com.criticalflow.global.auth.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes!!";
    private static final long EXPIRATION = 3600000L;
    private static final long REFRESH_EXPIRATION = 1209600000L;
    private static final Long USER_ID = 1L;

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, EXPIRATION, REFRESH_EXPIRATION);
    }

    @Nested
    @DisplayName("AccessToken 발급")
    class GenerateTokenTest {

        @Test
        @DisplayName("userId로 생성한 토큰을 파싱하면 동일한 userId가 반환된다")
        void returnsCorrectUserId() {
            String token = jwtProvider.generateToken(USER_ID);

            assertThat(jwtProvider.getUserId(token)).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("생성된 AccessToken은 유효성 검증을 통과한다")
        void isValid() {
            String token = jwtProvider.generateToken(USER_ID);

            assertThat(jwtProvider.validateToken(token)).isTrue();
        }
    }

    @Nested
    @DisplayName("RefreshToken 발급")
    class GenerateRefreshTokenTest {

        @Test
        @DisplayName("userId로 생성한 RefreshToken을 파싱하면 동일한 userId가 반환된다")
        void returnsCorrectUserId() {
            String token = jwtProvider.generateRefreshToken(USER_ID);

            assertThat(jwtProvider.getUserId(token)).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("생성된 RefreshToken은 유효성 검증을 통과한다")
        void isValid() {
            String token = jwtProvider.generateRefreshToken(USER_ID);

            assertThat(jwtProvider.validateToken(token)).isTrue();
        }
    }

    @Nested
    @DisplayName("토큰 검증")
    class ValidateTokenTest {

        @Test
        @DisplayName("변조된 토큰은 false를 반환한다")
        void returnsFalseForTamperedToken() {
            String token = jwtProvider.generateToken(USER_ID);
            String tampered = token.substring(0, token.length() - 5) + "XXXXX";

            assertThat(jwtProvider.validateToken(tampered)).isFalse();
        }

        @Test
        @DisplayName("빈 문자열 토큰은 false를 반환한다")
        void returnsFalseForEmptyToken() {
            assertThat(jwtProvider.validateToken("")).isFalse();
        }

        @Test
        @DisplayName("만료된 토큰은 false를 반환한다")
        void returnsFalseForExpiredToken() {
            JwtProvider shortLived = new JwtProvider(SECRET, 1L, REFRESH_EXPIRATION);
            String token = shortLived.generateToken(USER_ID);

            assertThat(shortLived.validateToken(token)).isFalse();
        }
    }

    @Nested
    @DisplayName("userId 추출")
    class GetUserIdTest {

        @Test
        @DisplayName("AccessToken에서 추출한 userId가 원본과 일치한다")
        void extractsCorrectUserIdFromAccessToken() {
            Long targetId = 42L;
            String token = jwtProvider.generateToken(targetId);

            assertThat(jwtProvider.getUserId(token)).isEqualTo(targetId);
        }

        @Test
        @DisplayName("RefreshToken에서 추출한 userId가 원본과 일치한다")
        void extractsCorrectUserIdFromRefreshToken() {
            Long targetId = 99L;
            String token = jwtProvider.generateRefreshToken(targetId);

            assertThat(jwtProvider.getUserId(token)).isEqualTo(targetId);
        }
    }
}

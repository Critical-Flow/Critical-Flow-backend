package com.criticalflow.domain.auth.service;

import com.criticalflow.domain.auth.entity.RefreshToken;
import com.criticalflow.domain.auth.repository.RefreshTokenRepository;
import com.criticalflow.global.auth.jwt.JwtProvider;
import com.criticalflow.global.exception.DomainException;
import com.criticalflow.global.exception.ErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    @Transactional
    public void logout(Long userId, HttpServletResponse response) {
        refreshTokenRepository.deleteByUserId(userId);
        clearRefreshTokenCookie(response);
    }

    @Transactional
    public String reissue(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenValue(refreshTokenValue)
                .orElseThrow(() -> new DomainException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new DomainException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        return jwtProvider.generateToken(refreshToken.getUserId());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}

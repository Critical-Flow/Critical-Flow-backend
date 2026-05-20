package com.criticalflow.global.auth;

import com.criticalflow.domain.auth.entity.RefreshToken;
import com.criticalflow.domain.auth.repository.RefreshTokenRepository;
import com.criticalflow.domain.user.entity.User;
import com.criticalflow.domain.user.repository.UserRepository;
import com.criticalflow.global.auth.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final String frontendCallbackUrl;

    public OAuth2SuccessHandler(
            JwtProvider jwtProvider,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            @Value("${frontend.callback-url}") String frontendCallbackUrl
    ) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.frontendCallbackUrl = frontendCallbackUrl;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Long githubId = ((Number) oAuth2User.getAttribute("id")).longValue();

        User user = userRepository.findByGithubId(githubId)
                .orElseThrow(() -> new IllegalStateException("OAuth2 로그인 성공 후 유저를 찾을 수 없습니다."));

        String accessToken = jwtProvider.generateToken(user.getUserId());
        String refreshTokenValue = jwtProvider.generateRefreshToken(user.getUserId());

        refreshTokenRepository.deleteByUserId(user.getUserId());
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getUserId())
                .tokenValue(refreshTokenValue)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtProvider.getRefreshExpiration() / 1000))
                .build());

        Cookie cookie = new Cookie("refresh_token", refreshTokenValue);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtProvider.getRefreshExpiration() / 1000));
        response.addCookie(cookie);

        getRedirectStrategy().sendRedirect(request, response, frontendCallbackUrl + "?token=" + accessToken);
    }
}

package com.criticalflow.global.auth;

import com.criticalflow.domain.user.entity.User;
import com.criticalflow.domain.user.repository.UserRepository;
import com.criticalflow.global.auth.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Long githubId = ((Number) oAuth2User.getAttribute("id")).longValue();

        User user = userRepository.findByGithubId(githubId)
                .orElseThrow(() -> new IllegalStateException("OAuth2 로그인 성공 후 유저를 찾을 수 없습니다."));

        String token = jwtProvider.generateToken(user.getUserId());

        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"token\":\"" + token + "\"}");
    }
}

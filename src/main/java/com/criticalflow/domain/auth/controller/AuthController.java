package com.criticalflow.domain.auth.controller;

import com.criticalflow.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Auth", description = "인증 API — 로그아웃 및 AccessToken 재발급")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "로그아웃",
            description = "서버에 저장된 RefreshToken을 삭제하고 쿠키를 만료시킵니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Long userId,
            HttpServletResponse response
    ) {
        authService.logout(userId, response);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "AccessToken 재발급",
            description = "HttpOnly 쿠키의 RefreshToken을 검증하여 새 AccessToken을 발급합니다. 인증 불필요."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AccessToken 재발급 성공"),
            @ApiResponse(responseCode = "401", description = "RefreshToken 쿠키 없음, 유효하지 않음 또는 만료됨")
    })
    @PostMapping("/reissue")
    public ResponseEntity<Map<String, String>> reissue(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null) {
            return ResponseEntity.status(401).build();
        }
        String newAccessToken = authService.reissue(refreshToken);
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }
}

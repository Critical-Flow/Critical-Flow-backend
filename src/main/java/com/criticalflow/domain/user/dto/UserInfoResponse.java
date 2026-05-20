package com.criticalflow.domain.user.dto;

import com.criticalflow.domain.user.entity.User;

import java.time.LocalDateTime;

public record UserInfoResponse(
        Long userId,
        String name,
        String email,
        String affiliation,
        LocalDateTime createdAt
) {
    public static UserInfoResponse from(User user) {
        return new UserInfoResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getAffiliation(),
                user.getCreatedAt()
        );
    }
}

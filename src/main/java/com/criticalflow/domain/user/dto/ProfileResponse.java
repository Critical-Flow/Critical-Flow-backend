package com.criticalflow.domain.user.dto;

import com.criticalflow.domain.user.entity.User;

public record ProfileResponse(
        String name,
        String affiliation
) {
    public static ProfileResponse from(User user) {
        return new ProfileResponse(user.getName(), user.getAffiliation());
    }
}

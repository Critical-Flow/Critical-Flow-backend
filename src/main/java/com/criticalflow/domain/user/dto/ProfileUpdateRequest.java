package com.criticalflow.domain.user.dto;

public record ProfileUpdateRequest(
        String name,
        String affiliation
) {
}

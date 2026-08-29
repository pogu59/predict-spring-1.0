package com.predict.controller.dto;

import com.predict.User;
import com.predict.enums.Tier;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String nickname,
        String signupChannel,
        int credibilityScore,
        Tier tier,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getNickname(), user.getSignupChannel(),
                user.getCredibilityScore(), user.getTier(), user.getCreatedAt());
    }
}

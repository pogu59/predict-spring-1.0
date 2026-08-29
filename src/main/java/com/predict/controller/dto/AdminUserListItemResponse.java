package com.predict.controller.dto;

import com.predict.User;
import com.predict.enums.Role;
import com.predict.enums.Tier;

import java.time.LocalDateTime;

public record AdminUserListItemResponse(
        Long id,
        String nickname,
        Tier tier,
        int credibilityScore,
        Role role,
        LocalDateTime createdAt
) {
    public static AdminUserListItemResponse from(User user) {
        return new AdminUserListItemResponse(user.getId(), user.getNickname(), user.getTier(),
                user.getCredibilityScore(), user.getRole(), user.getCreatedAt());
    }
}

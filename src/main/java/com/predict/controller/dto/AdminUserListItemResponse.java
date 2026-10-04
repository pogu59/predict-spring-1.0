package com.predict.controller.dto;

import com.predict.User;
import com.predict.enums.Role;
import com.predict.enums.Tier;

import java.time.LocalDateTime;

/** via: 가입 경로(users.signup_channel — kakao/email 등). email은 이메일 가입 유저만 채워진다. */
public record AdminUserListItemResponse(
        Long id,
        String nickname,
        String email,
        String via,
        Tier tier,
        int credibilityScore,
        Role role,
        boolean suspended,
        LocalDateTime createdAt
) {
    public static AdminUserListItemResponse from(User user) {
        return new AdminUserListItemResponse(user.getId(), user.getNickname(), user.getEmail(), user.getSignupChannel(),
                user.getTier(), user.getCredibilityScore(), user.getRole(), user.isSuspended(), user.getCreatedAt());
    }
}

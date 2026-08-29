package com.predict.controller.dto;

import com.predict.User;
import com.predict.enums.Role;
import com.predict.enums.Tier;

public record MeResponse(
        Long userId,
        String nickname,
        int credibilityScore,
        Tier tier,
        Role role
) {
    public static MeResponse from(User user) {
        return new MeResponse(user.getId(), user.getNickname(), user.getCredibilityScore(), user.getTier(), user.getRole());
    }
}

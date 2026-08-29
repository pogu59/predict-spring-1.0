package com.predict.controller.dto;

import com.predict.User;
import com.predict.enums.Role;
import com.predict.enums.Tier;

import java.time.LocalDateTime;

/** 정답률 = correctCount / gradedCount 는 프론트에서 계산한다(schema_8.sql 참고쿼리 ⑬). */
public record AdminUserDetailResponse(
        Long id,
        String nickname,
        Tier tier,
        int credibilityScore,
        Role role,
        boolean activitySuppressed,
        LocalDateTime createdAt,
        long totalVotes,
        long correctCount,
        long gradedCount
) {
    public static AdminUserDetailResponse from(User user, long totalVotes, long correctCount, long gradedCount) {
        return new AdminUserDetailResponse(user.getId(), user.getNickname(), user.getTier(),
                user.getCredibilityScore(), user.getRole(), user.isActivitySuppressed(), user.getCreatedAt(),
                totalVotes, correctCount, gradedCount);
    }
}

package com.predict.tier;

import com.predict.enums.Tier;

/**
 * 누적 신용도 점수 -> 티어 매핑 (docs/predict.md 3-1절 구간표).
 */
public final class TierPolicy {

    private TierPolicy() {
    }

    public static Tier fromScore(int credibilityScore) {
        if (credibilityScore >= 500) {
            return Tier.MASTER;
        }
        if (credibilityScore >= 400) {
            return Tier.DIAMOND;
        }
        if (credibilityScore >= 300) {
            return Tier.PLATINUM;
        }
        if (credibilityScore >= 200) {
            return Tier.GOLD;
        }
        if (credibilityScore >= 100) {
            return Tier.SILVER;
        }
        if (credibilityScore >= 1) {
            return Tier.BRONZE;
        }
        return Tier.UNRANKED;
    }

    /**
     * 활동성 강등 상태(users.activity_suppressed)를 반영한 실시간 티어 산정.
     * 점수만으로는 다이아/마스터 구간이어도 activitySuppressed=true면 플래티넘까지만 허용한다
     * (다이아/마스터 복귀는 오직 주간 활동성 체크(WeeklyActivityService)로만 가능).
     */
    public static Tier fromScore(int credibilityScore, boolean activitySuppressed) {
        Tier tier = fromScore(credibilityScore);
        if (activitySuppressed && (tier == Tier.DIAMOND || tier == Tier.MASTER)) {
            return Tier.PLATINUM;
        }
        return tier;
    }

    /** 다이아/마스터 주간 활동성 미달 시 표시 티어를 한 단계 낮출 때만 사용한다. */
    public static Tier oneStepDown(Tier tier) {
        return switch (tier) {
            case MASTER -> Tier.DIAMOND;
            case DIAMOND -> Tier.PLATINUM;
            default -> throw new IllegalArgumentException("활동성 강등 대상이 아닌 티어: " + tier);
        };
    }
}

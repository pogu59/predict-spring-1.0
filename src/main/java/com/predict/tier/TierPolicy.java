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

    /** 다이아/마스터 주간 활동성 미달 시 표시 티어를 한 단계 낮출 때만 사용한다. */
    public static Tier oneStepDown(Tier tier) {
        return switch (tier) {
            case MASTER -> Tier.DIAMOND;
            case DIAMOND -> Tier.PLATINUM;
            default -> throw new IllegalArgumentException("활동성 강등 대상이 아닌 티어: " + tier);
        };
    }
}

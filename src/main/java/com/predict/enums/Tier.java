package com.predict.enums;

/**
 * 유저 티어. users.tier, tier_changes.*, weekly_activity_snapshots.tier_* 컬럼은
 * VARCHAR(20)에 한글 라벨을 그대로 저장하므로, DB 값은 {@link #getDbValue()}로 왕복한다.
 */
public enum Tier {
    UNRANKED("언랭크"),
    BRONZE("브론즈"),
    SILVER("실버"),
    GOLD("골드"),
    PLATINUM("플래티넘"),
    DIAMOND("다이아"),
    MASTER("마스터");

    private final String dbValue;

    Tier(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static Tier fromDbValue(String dbValue) {
        for (Tier tier : values()) {
            if (tier.dbValue.equals(dbValue)) {
                return tier;
            }
        }
        throw new IllegalArgumentException("알 수 없는 tier 값: " + dbValue);
    }
}

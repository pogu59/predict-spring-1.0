package com.predict.enums;

/**
 * tier_changes.reason.
 * natural_promotion/demotion = 점수 변동에 따른 자연 승급/강등
 * activity_demotion/restoration = 다이아·마스터 활동성 조건에 의한 강등/복귀
 * correction = 관리자의 오확정 정정으로 인한 재계산 결과 변경
 */
public enum TierChangeReason {
    NATURAL_PROMOTION("natural_promotion"),
    NATURAL_DEMOTION("natural_demotion"),
    ACTIVITY_DEMOTION("activity_demotion"),
    ACTIVITY_RESTORATION("activity_restoration"),
    CORRECTION("correction");

    private final String dbValue;

    TierChangeReason(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static TierChangeReason fromDbValue(String dbValue) {
        for (TierChangeReason reason : values()) {
            if (reason.dbValue.equals(dbValue)) {
                return reason;
            }
        }
        throw new IllegalArgumentException("알 수 없는 tier change reason 값: " + dbValue);
    }
}

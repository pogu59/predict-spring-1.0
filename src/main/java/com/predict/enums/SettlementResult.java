package com.predict.enums;

/**
 * score_settlements.result. MySQL ENUM('correct','incorrect')과 1:1 대응.
 */
public enum SettlementResult {
    CORRECT("correct"),
    INCORRECT("incorrect");

    private final String dbValue;

    SettlementResult(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static SettlementResult fromDbValue(String dbValue) {
        for (SettlementResult result : values()) {
            if (result.dbValue.equals(dbValue)) {
                return result;
            }
        }
        throw new IllegalArgumentException("알 수 없는 settlement result 값: " + dbValue);
    }
}

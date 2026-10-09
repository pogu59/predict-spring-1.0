package com.predict.enums;

/**
 * missions.type. attendance=출석 체크(문항 없음, 하루 1회), balance=밸런스 게임(문항 1개, 보기 2개),
 * survey=설문(문항 여러 개).
 */
public enum MissionType {
    ATTENDANCE("attendance"),
    BALANCE("balance"),
    SURVEY("survey");

    private final String dbValue;

    MissionType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static MissionType fromDbValue(String dbValue) {
        for (MissionType type : values()) {
            if (type.dbValue.equals(dbValue)) {
                return type;
            }
        }
        throw new IllegalArgumentException("알 수 없는 mission type 값: " + dbValue);
    }
}

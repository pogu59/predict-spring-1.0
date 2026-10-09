package com.predict.enums;

/**
 * missions.status. draft=작성 중(참여자에게 안 보임), open=공개, closed=관리자가 닫음.
 * 공개 기간(starts_at~ends_at)이 지나면 status와 무관하게 참여할 수 없다(Mission.isAvailableAt).
 */
public enum MissionStatus {
    DRAFT("draft"),
    OPEN("open"),
    CLOSED("closed");

    private final String dbValue;

    MissionStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static MissionStatus fromDbValue(String dbValue) {
        for (MissionStatus status : values()) {
            if (status.dbValue.equals(dbValue)) {
                return status;
            }
        }
        throw new IllegalArgumentException("알 수 없는 mission status 값: " + dbValue);
    }
}

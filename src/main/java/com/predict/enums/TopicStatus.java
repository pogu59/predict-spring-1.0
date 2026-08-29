package com.predict.enums;

/**
 * topics.status. MySQL ENUM('open','pending_result','confirmed','void')과 1:1 대응.
 */
public enum TopicStatus {
    OPEN("open"),
    PENDING_RESULT("pending_result"),
    CONFIRMED("confirmed"),
    VOID("void");

    private final String dbValue;

    TopicStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static TopicStatus fromDbValue(String dbValue) {
        for (TopicStatus status : values()) {
            if (status.dbValue.equals(dbValue)) {
                return status;
            }
        }
        throw new IllegalArgumentException("알 수 없는 topic status 값: " + dbValue);
    }
}

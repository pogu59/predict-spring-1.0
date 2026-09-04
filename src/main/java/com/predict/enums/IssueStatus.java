package com.predict.enums;

/**
 * issues.status. MySQL ENUM('open','pending_result','confirmed')과 1:1 대응.
 */
public enum IssueStatus {
    OPEN("open"),
    PENDING_RESULT("pending_result"),
    CONFIRMED("confirmed");

    private final String dbValue;

    IssueStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static IssueStatus fromDbValue(String dbValue) {
        for (IssueStatus status : values()) {
            if (status.dbValue.equals(dbValue)) {
                return status;
            }
        }
        throw new IllegalArgumentException("알 수 없는 issue status 값: " + dbValue);
    }
}

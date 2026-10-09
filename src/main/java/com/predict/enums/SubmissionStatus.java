package com.predict.enums;

/**
 * mission_submissions.status. MVP는 제출 즉시 규칙 검수(QualityPolicy)로 approved/rejected가 정해진다.
 * pending은 v1의 사진·영수증 미션(사람·AI 검수 대기)용으로 자리만 잡아 둔다.
 */
public enum SubmissionStatus {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected");

    private final String dbValue;

    SubmissionStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static SubmissionStatus fromDbValue(String dbValue) {
        for (SubmissionStatus status : values()) {
            if (status.dbValue.equals(dbValue)) {
                return status;
            }
        }
        throw new IllegalArgumentException("알 수 없는 submission status 값: " + dbValue);
    }
}

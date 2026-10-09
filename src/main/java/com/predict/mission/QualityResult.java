package com.predict.mission;

import com.predict.enums.SubmissionStatus;

/**
 * 제출 한 건의 검수 결과. score는 0~100(분석용 기록), rejectReason은 반려일 때만 채워진다.
 */
public record QualityResult(int score, SubmissionStatus status, String rejectReason) {

    public static QualityResult approved(int score) {
        return new QualityResult(Math.max(0, Math.min(100, score)), SubmissionStatus.APPROVED, null);
    }

    public static QualityResult rejected(String reason) {
        return new QualityResult(0, SubmissionStatus.REJECTED, reason);
    }
}

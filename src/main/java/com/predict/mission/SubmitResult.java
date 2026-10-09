package com.predict.mission;

import com.predict.MissionSubmission;

/**
 * 제출 결과. earnedPoints는 이번 미션 적립, bonusPoints는 오늘의 미션을 모두 끝내 받은 보너스
 * (없으면 0), balance는 처리 후 지갑 잔액.
 */
public record SubmitResult(MissionSubmission submission, int earnedPoints, int bonusPoints, int balance) {
}

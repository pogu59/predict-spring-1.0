package com.predict.mission;

import com.predict.MissionQuestion;

import java.util.List;

/**
 * 문항 하나의 응답 비율. 참여 인원은 공개하지 않고 비율(0~100 정수, 합 100)만 내려준다
 * (예측 이슈와 같은 규칙, VotePercentages.ofCounts). myAnswer는 내가 고른 보기 인덱스.
 */
public record QuestionResult(MissionQuestion question, List<Integer> percents, Integer myAnswer) {
}

package com.predict.controller.dto;

/** 마이페이지 요약 통계. 정답률 = correctCount / gradedCount 는 프론트에서 계산한다. */
public record MyStatsResponse(
        long totalVotes,
        long correctCount,
        long gradedCount
) {
}

package com.predict.controller.dto;

import com.predict.Vote;
import com.predict.enums.Choice;

import java.time.LocalDateTime;

/**
 * liveYesCount/liveNoCount는 투표 직후 그 순간의 실시간 집계로, 투표한 본인에게만 1회성으로 응답된다.
 * 별도의 공개 조회 엔드포인트는 없다(docs/predict.md 2-6절 — 마감 전 전체 공개 금지).
 */
public record VoteResponse(
        Long id,
        Long topicId,
        Choice choice,
        LocalDateTime votedAt,
        int liveYesCount,
        int liveNoCount
) {
    public static VoteResponse of(Vote vote, int liveYesCount, int liveNoCount) {
        return new VoteResponse(vote.getId(), vote.getTopic().getId(), vote.getChoice(), vote.getVotedAt(),
                liveYesCount, liveNoCount);
    }
}

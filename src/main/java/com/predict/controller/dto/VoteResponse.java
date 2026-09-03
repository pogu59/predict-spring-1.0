package com.predict.controller.dto;

import com.predict.Vote;

import java.time.LocalDateTime;
import java.util.List;

/**
 * liveCounts는 투표 직후 그 순간의 선택지별 실시간 집계로, 투표한 본인에게만 1회성으로 응답된다.
 * 별도의 공개 조회 엔드포인트는 없다(docs/predict.md 2-6절 — 마감 전 전체 공개 금지).
 */
public record VoteResponse(
        Long id,
        Long topicId,
        Long optionId,
        LocalDateTime votedAt,
        List<TopicOptionResponse> liveCounts
) {
    public static VoteResponse of(Vote vote, List<TopicOptionResponse> liveCounts) {
        return new VoteResponse(vote.getId(), vote.getTopic().getId(), vote.getTopicOption().getId(),
                vote.getVotedAt(), liveCounts);
    }
}

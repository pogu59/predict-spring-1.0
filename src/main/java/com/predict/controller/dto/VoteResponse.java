package com.predict.controller.dto;

import com.predict.Vote;

import java.time.LocalDateTime;
import java.util.List;

/**
 * liveCounts는 투표 직후 그 순간의 선택지별 실시간 집계로, 투표한 본인에게만 1회성으로 응답된다.
 * 별도의 공개 조회 엔드포인트는 없다(docs/predict.md 2-6절 — 마감 전 전체 공개 금지).
 * remainingCredibility: 이 투표로 stake를 에스크로하고 난 직후의 잔여 신용도 — 프론트가
 * 별도로 /api/auth/me를 다시 부르지 않아도 즉시 잔액을 반영할 수 있게 함께 내려준다.
 */
public record VoteResponse(
        Long id,
        Long issueId,
        Long optionId,
        int stake,
        int remainingCredibility,
        LocalDateTime votedAt,
        List<IssueOptionResponse> liveCounts
) {
    public static VoteResponse of(Vote vote, List<IssueOptionResponse> liveCounts, int remainingCredibility) {
        return new VoteResponse(vote.getId(), vote.getIssue().getId(), vote.getIssueOption().getId(),
                vote.getStake(), remainingCredibility, vote.getVotedAt(), liveCounts);
    }
}

package com.predict.controller.dto;

import com.predict.Vote;

import java.time.LocalDateTime;
import java.util.List;

/**
 * remainingCredibility: 이 투표로 stake를 에스크로하고 난 직후의 잔여 신용도 — 프론트가
 * 별도로 /api/auth/me를 다시 부르지 않아도 즉시 잔액을 반영할 수 있게 함께 내려준다.
 * liveCounts: 투표 직후 선택지별 비율(이름은 호환을 위해 유지, 득표수는 싣지 않는다).
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

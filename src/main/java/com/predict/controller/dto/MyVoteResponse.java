package com.predict.controller.dto;

import com.predict.ScoreSettlement;
import com.predict.Vote;
import com.predict.enums.IssueStatus;
import com.predict.enums.SettlementResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 마이페이지 "내 예측" 한 줄. 선택지는 비율(percent)만 싣는다. */
public record MyVoteResponse(
        Long voteId,
        Long issueId,
        String title,
        IssueStatus status,
        Long optionId,
        String optionText,
        int stake,
        LocalDateTime votedAt,
        LocalDateTime voteDeadlineAt,
        LocalDateTime confirmedAt,
        Long correctOptionId,
        List<IssueOptionResponse> options,
        String coverImageUrl,
        SettlementResult result,
        Integer scoreDelta
) {
    public static MyVoteResponse from(Vote vote, Optional<ScoreSettlement> settlement, Map<Long, Integer> countsByOptionId) {
        var issue = vote.getIssue();
        return new MyVoteResponse(
                vote.getId(),
                issue.getId(),
                issue.getTitle(),
                issue.getStatus(),
                vote.getIssueOption().getId(),
                vote.getIssueOption().getText(),
                vote.getStake(),
                vote.getVotedAt(),
                issue.getVoteDeadlineAt(),
                issue.getConfirmedAt(),
                issue.getCorrectOption() != null ? issue.getCorrectOption().getId() : null,
                IssueOptionResponse.percentsOf(issue.getOptions(), countsByOptionId),
                issue.getCoverImageUrl(),
                settlement.map(ScoreSettlement::getResult).orElse(null),
                settlement.map(ScoreSettlement::getScoreDelta).orElse(null));
    }
}

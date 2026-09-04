package com.predict.controller.dto;

import com.predict.ScoreSettlement;
import com.predict.Vote;
import com.predict.enums.SettlementResult;
import com.predict.enums.IssueStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 마이페이지 "최근 투표 기록" 한 줄. 여기 담기는 항목은 전부 본인이 이미 투표한 주제이므로,
 * status가 OPEN이어도 득표수를 감추지 않는다 — "투표 완료 직후 본인에게 즉시 실시간 비율
 * 노출"(docs/predict.md 2-6절) 원칙. 아직 투표하지 않은 제3자에게 감추는 공개 IssueResponse와는
 * 다르다. IssueOption.voteCount 컬럼은 마감 시점 스냅샷이라 OPEN 동안은 항상 null이므로,
 * OPEN인 동안은 liveCounts(실시간 집계)를 대신 쓴다.
 */
public record MyVoteResponse(
        Long voteId,
        Long issueId,
        Integer categoryId,
        String categoryName,
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
        SettlementResult result,
        Integer scoreDelta
) {
    /** liveCounts: issue가 아직 OPEN일 때 쓰는, optionId -> 실시간 득표수 집계. OPEN이 아니면 무시된다. */
    public static MyVoteResponse from(Vote vote, Optional<ScoreSettlement> settlement, Map<Long, Integer> liveCounts) {
        var issue = vote.getIssue();
        boolean open = issue.getStatus() == IssueStatus.OPEN;
        return new MyVoteResponse(
                vote.getId(),
                issue.getId(),
                issue.getCategory().getId(),
                issue.getCategory().getName(),
                issue.getTitle(),
                issue.getStatus(),
                vote.getIssueOption().getId(),
                vote.getIssueOption().getText(),
                vote.getStake(),
                vote.getVotedAt(),
                issue.getVoteDeadlineAt(),
                issue.getConfirmedAt(),
                issue.getCorrectOption() != null ? issue.getCorrectOption().getId() : null,
                issue.getOptions().stream()
                        .map(option -> new IssueOptionResponse(option.getId(), option.getText(),
                                open ? liveCounts.getOrDefault(option.getId(), 0) : option.getVoteCount()))
                        .toList(),
                settlement.map(ScoreSettlement::getResult).orElse(null),
                settlement.map(ScoreSettlement::getScoreDelta).orElse(null));
    }
}

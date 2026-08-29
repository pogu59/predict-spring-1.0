package com.predict.controller.dto;

import com.predict.ScoreSettlement;
import com.predict.Vote;
import com.predict.enums.Choice;
import com.predict.enums.SettlementResult;
import com.predict.enums.TopicStatus;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 마이페이지 "최근 투표 기록" 한 줄. status가 OPEN이면 득표수를 감춰
 * 공개 TopicResponse와 동일한 비공개 원칙(docs/predict.md 2-6절)을 유지한다.
 */
public record MyVoteResponse(
        Long voteId,
        Long topicId,
        Integer categoryId,
        String categoryName,
        String title,
        TopicStatus status,
        Choice choice,
        LocalDateTime votedAt,
        LocalDateTime voteDeadlineAt,
        LocalDateTime confirmedAt,
        Choice correctAnswer,
        Integer yesCount,
        Integer noCount,
        SettlementResult result,
        Integer scoreDelta
) {
    public static MyVoteResponse from(Vote vote, Optional<ScoreSettlement> settlement) {
        var topic = vote.getTopic();
        boolean countsVisible = topic.getStatus() != TopicStatus.OPEN;
        return new MyVoteResponse(
                vote.getId(),
                topic.getId(),
                topic.getCategory().getId(),
                topic.getCategory().getName(),
                topic.getTitle(),
                topic.getStatus(),
                vote.getChoice(),
                vote.getVotedAt(),
                topic.getVoteDeadlineAt(),
                topic.getConfirmedAt(),
                topic.getCorrectAnswer(),
                countsVisible ? topic.getYesCount() : null,
                countsVisible ? topic.getNoCount() : null,
                settlement.map(ScoreSettlement::getResult).orElse(null),
                settlement.map(ScoreSettlement::getScoreDelta).orElse(null));
    }
}

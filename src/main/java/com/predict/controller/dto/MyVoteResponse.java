package com.predict.controller.dto;

import com.predict.ScoreSettlement;
import com.predict.Vote;
import com.predict.enums.SettlementResult;
import com.predict.enums.TopicStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 마이페이지 "최근 투표 기록" 한 줄. 여기 담기는 항목은 전부 본인이 이미 투표한 주제이므로,
 * status가 OPEN이어도 득표수를 감추지 않는다 — "투표 완료 직후 본인에게 즉시 실시간 비율
 * 노출"(docs/predict.md 2-6절) 원칙. 아직 투표하지 않은 제3자에게 감추는 공개 TopicResponse와는
 * 다르다. TopicOption.voteCount 컬럼은 마감 시점 스냅샷이라 OPEN 동안은 항상 null이므로,
 * OPEN인 동안은 liveCounts(실시간 집계)를 대신 쓴다.
 */
public record MyVoteResponse(
        Long voteId,
        Long topicId,
        Integer categoryId,
        String categoryName,
        String title,
        TopicStatus status,
        Long optionId,
        String optionText,
        LocalDateTime votedAt,
        LocalDateTime voteDeadlineAt,
        LocalDateTime confirmedAt,
        Long correctOptionId,
        List<TopicOptionResponse> options,
        SettlementResult result,
        Integer scoreDelta
) {
    /** liveCounts: topic이 아직 OPEN일 때 쓰는, optionId -> 실시간 득표수 집계. OPEN이 아니면 무시된다. */
    public static MyVoteResponse from(Vote vote, Optional<ScoreSettlement> settlement, Map<Long, Integer> liveCounts) {
        var topic = vote.getTopic();
        boolean open = topic.getStatus() == TopicStatus.OPEN;
        return new MyVoteResponse(
                vote.getId(),
                topic.getId(),
                topic.getCategory().getId(),
                topic.getCategory().getName(),
                topic.getTitle(),
                topic.getStatus(),
                vote.getTopicOption().getId(),
                vote.getTopicOption().getText(),
                vote.getVotedAt(),
                topic.getVoteDeadlineAt(),
                topic.getConfirmedAt(),
                topic.getCorrectOption() != null ? topic.getCorrectOption().getId() : null,
                topic.getOptions().stream()
                        .map(option -> new TopicOptionResponse(option.getId(), option.getText(),
                                open ? liveCounts.getOrDefault(option.getId(), 0) : option.getVoteCount()))
                        .toList(),
                settlement.map(ScoreSettlement::getResult).orElse(null),
                settlement.map(ScoreSettlement::getScoreDelta).orElse(null));
    }
}

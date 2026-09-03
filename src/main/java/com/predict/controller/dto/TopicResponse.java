package com.predict.controller.dto;

import com.predict.Topic;
import com.predict.enums.TopicStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 선택지별 득표수는 status가 OPEN이면 null로 감춘다.
 * 마감 전 실시간 득표비율 비공개 원칙(docs/predict.md 2-6절)을 응답 형태로 강제한다.
 */
public record TopicResponse(
        Long id,
        Integer categoryId,
        String title,
        String description,
        TopicStatus status,
        LocalDateTime voteStartAt,
        LocalDateTime voteDeadlineAt,
        LocalDateTime confirmedAt,
        Long correctOptionId,
        List<TopicOptionResponse> options,
        LocalDateTime createdAt
) {
    public static TopicResponse from(Topic topic) {
        boolean countsVisible = topic.getStatus() != TopicStatus.OPEN;
        return new TopicResponse(
                topic.getId(),
                topic.getCategory().getId(),
                topic.getTitle(),
                topic.getDescription(),
                topic.getStatus(),
                topic.getVoteStartAt(),
                topic.getVoteDeadlineAt(),
                topic.getConfirmedAt(),
                topic.getCorrectOption() != null ? topic.getCorrectOption().getId() : null,
                topic.getOptions().stream()
                        .map(option -> new TopicOptionResponse(option.getId(), option.getText(),
                                countsVisible ? option.getVoteCount() : null))
                        .toList(),
                topic.getCreatedAt());
    }
}

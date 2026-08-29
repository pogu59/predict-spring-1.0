package com.predict.controller.dto;

import com.predict.Topic;
import com.predict.enums.Choice;
import com.predict.enums.TopicStatus;

import java.time.LocalDateTime;

/**
 * yesCount/noCount는 status가 OPEN이면 null로 감춘다.
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
        Choice correctAnswer,
        Integer yesCount,
        Integer noCount,
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
                topic.getCorrectAnswer(),
                countsVisible ? topic.getYesCount() : null,
                countsVisible ? topic.getNoCount() : null,
                topic.getCreatedAt());
    }
}

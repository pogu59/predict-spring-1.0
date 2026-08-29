package com.predict.controller.dto;

import com.predict.Topic;
import com.predict.enums.TopicStatus;

import java.time.LocalDateTime;

/**
 * 관리자 페이지 주제 목록. 공개 TopicResponse와 달리 상태와 무관하게 득표수를 항상 노출한다.
 */
public record AdminTopicListItemResponse(
        Long id,
        Integer categoryId,
        String categoryName,
        String title,
        TopicStatus status,
        LocalDateTime voteStartAt,
        LocalDateTime voteDeadlineAt,
        int yesCount,
        int noCount,
        long totalVotes
) {
    public static AdminTopicListItemResponse from(Topic topic, long totalVotes) {
        return new AdminTopicListItemResponse(
                topic.getId(),
                topic.getCategory().getId(),
                topic.getCategory().getName(),
                topic.getTitle(),
                topic.getStatus(),
                topic.getVoteStartAt(),
                topic.getVoteDeadlineAt(),
                topic.getYesCount(),
                topic.getNoCount(),
                totalVotes);
    }
}

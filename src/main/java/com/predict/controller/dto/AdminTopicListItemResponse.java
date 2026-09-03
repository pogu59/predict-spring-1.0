package com.predict.controller.dto;

import com.predict.Topic;
import com.predict.enums.TopicStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 페이지 주제 목록. 공개 TopicResponse와 달리 상태와 무관하게 선택지 정보를 항상 노출한다.
 */
public record AdminTopicListItemResponse(
        Long id,
        Integer categoryId,
        String categoryName,
        String title,
        TopicStatus status,
        LocalDateTime voteStartAt,
        LocalDateTime voteDeadlineAt,
        List<TopicOptionResponse> options,
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
                topic.getOptions().stream()
                        .map(option -> new TopicOptionResponse(option.getId(), option.getText(), option.getVoteCount()))
                        .toList(),
                totalVotes);
    }
}

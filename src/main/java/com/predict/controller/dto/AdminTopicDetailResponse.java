package com.predict.controller.dto;

import com.predict.Topic;
import com.predict.enums.TopicStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AdminTopicDetailResponse(
        Long id,
        Integer categoryId,
        String categoryName,
        String title,
        String description,
        TopicStatus status,
        LocalDateTime voteStartAt,
        LocalDateTime voteDeadlineAt,
        LocalDateTime confirmedAt,
        Long confirmedByUserId,
        String confirmedByNickname,
        Long correctOptionId,
        List<TopicOptionResponse> options,
        long totalVotes,
        boolean canFullEdit,
        boolean canExtendDeadline,
        LocalDateTime createdAt
) {
    public static AdminTopicDetailResponse from(Topic topic, long totalVotes) {
        boolean isOpen = topic.getStatus() == TopicStatus.OPEN;
        return new AdminTopicDetailResponse(
                topic.getId(),
                topic.getCategory().getId(),
                topic.getCategory().getName(),
                topic.getTitle(),
                topic.getDescription(),
                topic.getStatus(),
                topic.getVoteStartAt(),
                topic.getVoteDeadlineAt(),
                topic.getConfirmedAt(),
                topic.getConfirmedBy() != null ? topic.getConfirmedBy().getId() : null,
                topic.getConfirmedBy() != null ? topic.getConfirmedBy().getNickname() : null,
                topic.getCorrectOption() != null ? topic.getCorrectOption().getId() : null,
                topic.getOptions().stream()
                        .map(option -> new TopicOptionResponse(option.getId(), option.getText(), option.getVoteCount()))
                        .toList(),
                totalVotes,
                isOpen && totalVotes == 0,
                isOpen,
                topic.getCreatedAt());
    }
}

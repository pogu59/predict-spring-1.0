package com.predict.controller.dto;

import com.predict.Topic;
import com.predict.enums.Choice;
import com.predict.enums.TopicStatus;

import java.time.LocalDateTime;

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
        Choice correctAnswer,
        int yesCount,
        int noCount,
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
                topic.getCorrectAnswer(),
                topic.getYesCount(),
                topic.getNoCount(),
                totalVotes,
                isOpen && totalVotes == 0,
                isOpen,
                topic.getCreatedAt());
    }
}

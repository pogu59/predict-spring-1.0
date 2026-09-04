package com.predict.controller.dto;

import com.predict.Issue;
import com.predict.enums.IssueStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AdminIssueDetailResponse(
        Long id,
        Integer categoryId,
        String categoryName,
        String title,
        String description,
        IssueStatus status,
        LocalDateTime voteStartAt,
        LocalDateTime voteDeadlineAt,
        LocalDateTime confirmedAt,
        Long confirmedByUserId,
        String confirmedByNickname,
        Long correctOptionId,
        List<IssueOptionResponse> options,
        long totalVotes,
        boolean canFullEdit,
        boolean canExtendDeadline,
        LocalDateTime createdAt
) {
    public static AdminIssueDetailResponse from(Issue issue, long totalVotes) {
        boolean isOpen = issue.getStatus() == IssueStatus.OPEN;
        return new AdminIssueDetailResponse(
                issue.getId(),
                issue.getCategory().getId(),
                issue.getCategory().getName(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getStatus(),
                issue.getVoteStartAt(),
                issue.getVoteDeadlineAt(),
                issue.getConfirmedAt(),
                issue.getConfirmedBy() != null ? issue.getConfirmedBy().getId() : null,
                issue.getConfirmedBy() != null ? issue.getConfirmedBy().getNickname() : null,
                issue.getCorrectOption() != null ? issue.getCorrectOption().getId() : null,
                issue.getOptions().stream()
                        .map(option -> new IssueOptionResponse(option.getId(), option.getText(), option.getVoteCount()))
                        .toList(),
                totalVotes,
                isOpen && totalVotes == 0,
                isOpen,
                issue.getCreatedAt());
    }
}

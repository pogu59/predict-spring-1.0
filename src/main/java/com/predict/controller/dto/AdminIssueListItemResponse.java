package com.predict.controller.dto;

import com.predict.Issue;
import com.predict.enums.IssueStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 페이지 주제 목록. 공개 IssueResponse와 달리 상태와 무관하게 선택지 정보를 항상 노출한다.
 */
public record AdminIssueListItemResponse(
        Long id,
        Integer categoryId,
        String categoryName,
        String title,
        IssueStatus status,
        LocalDateTime voteStartAt,
        LocalDateTime voteDeadlineAt,
        List<IssueOptionResponse> options,
        long totalVotes
) {
    public static AdminIssueListItemResponse from(Issue issue, long totalVotes) {
        return new AdminIssueListItemResponse(
                issue.getId(),
                issue.getCategory().getId(),
                issue.getCategory().getName(),
                issue.getTitle(),
                issue.getStatus(),
                issue.getVoteStartAt(),
                issue.getVoteDeadlineAt(),
                issue.getOptions().stream()
                        .map(option -> new IssueOptionResponse(option.getId(), option.getText(), option.getVoteCount()))
                        .toList(),
                totalVotes);
    }
}

package com.predict.controller.dto;

import com.predict.Issue;
import com.predict.enums.IssueStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 관리자 이슈 목록. 관리자 화면도 참여 인원 없이 선택지별 비율만 보여준다. */
public record AdminIssueListItemResponse(
        Long id,
        String title,
        IssueStatus status,
        LocalDateTime voteStartAt,
        LocalDateTime voteDeadlineAt,
        Long correctOptionId,
        String coverImageUrl,
        List<IssueOptionResponse> options
) {
    public static AdminIssueListItemResponse from(Issue issue, Map<Long, Integer> countsByOptionId) {
        return new AdminIssueListItemResponse(
                issue.getId(),
                issue.getTitle(),
                issue.getStatus(),
                issue.getVoteStartAt(),
                issue.getVoteDeadlineAt(),
                issue.getCorrectOption() != null ? issue.getCorrectOption().getId() : null,
                issue.getCoverImageUrl(),
                IssueOptionResponse.percentsOf(issue.getOptions(), countsByOptionId));
    }
}

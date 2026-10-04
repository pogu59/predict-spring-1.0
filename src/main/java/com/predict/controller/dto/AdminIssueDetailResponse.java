package com.predict.controller.dto;

import com.predict.Issue;
import com.predict.enums.IssueStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * canFullEdit: 진행중이고 아직 참여자가 없을 때만 전체 수정 가능(AdminIssueService.updateIssue).
 * canExtendDeadline: 확정 전이면 연장 가능 — 결과대기 이슈를 미래로 연장하면 다시 진행중이 된다.
 */
public record AdminIssueDetailResponse(
        Long id,
        Integer categoryId,
        String title,
        String description,
        IssueStatus status,
        LocalDateTime voteStartAt,
        LocalDateTime voteDeadlineAt,
        LocalDateTime confirmedAt,
        Long confirmedByUserId,
        String confirmedByNickname,
        Long correctOptionId,
        String coverImageUrl,
        List<IssueOptionResponse> options,
        boolean canFullEdit,
        boolean canExtendDeadline,
        LocalDateTime createdAt
) {
    public static AdminIssueDetailResponse from(Issue issue, Map<Long, Integer> countsByOptionId, long totalVotes) {
        boolean isOpen = issue.getStatus() == IssueStatus.OPEN;
        return new AdminIssueDetailResponse(
                issue.getId(),
                issue.getCategory().getId(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getStatus(),
                issue.getVoteStartAt(),
                issue.getVoteDeadlineAt(),
                issue.getConfirmedAt(),
                issue.getConfirmedBy() != null ? issue.getConfirmedBy().getId() : null,
                issue.getConfirmedBy() != null ? issue.getConfirmedBy().getNickname() : null,
                issue.getCorrectOption() != null ? issue.getCorrectOption().getId() : null,
                issue.getCoverImageUrl(),
                IssueOptionResponse.percentsOf(issue.getOptions(), countsByOptionId),
                isOpen && totalVotes == 0,
                issue.getStatus() != IssueStatus.CONFIRMED,
                issue.getCreatedAt());
    }
}

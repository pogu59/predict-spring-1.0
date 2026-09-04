package com.predict.controller.dto;

import com.predict.Issue;
import com.predict.enums.IssueStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 선택지별 득표수는 status가 OPEN이면 감춘다 — 단, 요청한 유저(myOptionId)가 이미 이 주제에
 * 투표했다면 본인에게는 즉시 실시간으로 노출한다. 마감 전 득표비율 비공개 원칙
 * (docs/predict.md 2-6절)을 응답 형태로 강제하되, "투표 완료 직후 본인 노출" 예외를 반영한다.
 * IssueOption.voteCount 컬럼은 마감 시점 스냅샷이라 OPEN 동안은 항상 null이므로, 본인 노출
 * 케이스에서는 liveCounts(득표수 실시간 집계 결과)를 대신 사용한다.
 */
public record IssueResponse(
        Long id,
        Integer categoryId,
        String title,
        String description,
        IssueStatus status,
        LocalDateTime voteStartAt,
        LocalDateTime voteDeadlineAt,
        LocalDateTime confirmedAt,
        Long correctOptionId,
        List<IssueOptionResponse> options,
        LocalDateTime createdAt,
        Long myOptionId,
        Integer myStake
) {
    public static IssueResponse from(Issue issue) {
        return from(issue, null, null, null);
    }

    /**
     * myOptionId: 요청한 유저가 이 주제에 투표했다면 그 선택지 id, 아니면 null.
     * myStake: 그 투표에 건 신용도. myOptionId가 null이면 함께 null.
     * liveCounts: myOptionId가 있고 issue가 아직 OPEN일 때만 쓰이는, optionId -> 실시간 득표수 집계.
     */
    public static IssueResponse from(Issue issue, Long myOptionId, Integer myStake, Map<Long, Integer> liveCounts) {
        boolean revealOpen = issue.getStatus() == IssueStatus.OPEN && myOptionId != null;
        boolean countsVisible = issue.getStatus() != IssueStatus.OPEN || revealOpen;
        return new IssueResponse(
                issue.getId(),
                issue.getCategory().getId(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getStatus(),
                issue.getVoteStartAt(),
                issue.getVoteDeadlineAt(),
                issue.getConfirmedAt(),
                issue.getCorrectOption() != null ? issue.getCorrectOption().getId() : null,
                issue.getOptions().stream()
                        .map(option -> new IssueOptionResponse(option.getId(), option.getText(),
                                !countsVisible ? null
                                        : revealOpen ? liveCounts.getOrDefault(option.getId(), 0)
                                        : option.getVoteCount()))
                        .toList(),
                issue.getCreatedAt(),
                myOptionId,
                myStake);
    }
}

package com.predict.controller.dto;

import com.predict.Issue;
import com.predict.enums.IssueStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 공개 이슈 응답. 선택지별 비율(percent)은 상태와 무관하게 항상 공개하고(투표 전 포함),
 * 참여 인원·득표수는 어디에도 싣지 않는다.
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
        String coverImageUrl,
        LocalDateTime createdAt,
        Long myOptionId,
        Integer myStake
) {
    /**
     * countsByOptionId: VoteCountService가 집계한 선택지별 득표수(비율 계산에만 쓴다).
     * myOptionId/myStake: 요청한 유저가 이 주제에 투표했다면 그 선택지 id와 건 신용도, 아니면 null.
     */
    public static IssueResponse from(Issue issue, Map<Long, Integer> countsByOptionId, Long myOptionId, Integer myStake) {
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
                IssueOptionResponse.percentsOf(issue.getOptions(), countsByOptionId),
                issue.getCoverImageUrl(),
                issue.getCreatedAt(),
                myOptionId,
                myStake);
    }
}

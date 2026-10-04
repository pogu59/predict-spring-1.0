package com.predict.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * categoryId: UI에서 카테고리를 없앴으므로 선택값 — 비우면 가장 앞 카테고리로 저장한다(AdminIssueService).
 * coverImageUrl: 업로드 API(/api/uploads/images)가 돌려준 URL. 선택.
 */
public record IssueCreateRequest(
        Integer categoryId,
        @NotBlank @Size(max = 60, message = "제목은 60자 이하로 입력해 주세요.") String title,
        String description,
        @NotNull LocalDateTime voteStartAt,
        @NotNull LocalDateTime voteDeadlineAt,
        @NotNull @Size(min = 2, max = 6, message = "선택지는 2개 이상 6개 이하로 입력해 주세요.")
        List<@NotBlank @Size(max = 30) String> options,
        @Size(max = 500) String coverImageUrl
) {
}

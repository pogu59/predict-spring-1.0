package com.predict.controller.dto;

import com.predict.enums.MissionType;
import com.predict.mission.QuestionDraft;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/** 미션 참여·관리 요청 DTO 모음. */
public final class MissionRequests {

    private MissionRequests() {
    }

    /**
     * answers: 문항 순서대로 고른 보기 인덱스(출석은 빈 배열 또는 생략).
     * durationMs: 미션 화면을 연 뒤 제출까지 걸린 시간 — 너무 빠른 응답 판정에 쓴다.
     */
    public record SubmitRequest(List<@NotNull @Min(0) Integer> answers, @NotNull @Min(0) Long durationMs) {
    }

    public record QuestionRequest(
            @NotBlank @Size(max = 200) String text,
            @NotNull @Size(min = 2, max = 6) List<@NotBlank @Size(max = 100) String> options,
            @Min(0) Integer attentionAnswerIndex) {

        public QuestionDraft toDraft() {
            return new QuestionDraft(text, options, attentionAnswerIndex);
        }
    }

    public record AdminCreateRequest(
            @NotNull MissionType type,
            @NotBlank @Size(max = 100) String title,
            @Size(max = 500) String description,
            @NotNull @Min(0) @Max(100_000) Integer rewardPoints,
            boolean daily,
            @NotNull LocalDateTime startsAt,
            @NotNull LocalDateTime endsAt,
            @Valid List<QuestionRequest> questions,
            boolean openNow) {

        public List<QuestionDraft> questionDrafts() {
            return questions == null ? List.of() : questions.stream().map(QuestionRequest::toDraft).toList();
        }
    }
}

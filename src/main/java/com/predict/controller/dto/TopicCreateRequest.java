package com.predict.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record TopicCreateRequest(
        @NotNull Integer categoryId,
        @NotBlank String title,
        String description,
        @NotNull LocalDateTime voteStartAt,
        @NotNull LocalDateTime voteDeadlineAt,
        @NotNull @Size(min = 2, message = "선택지는 최소 2개 이상이어야 합니다.") List<@NotBlank String> options
) {
}

package com.predict.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TopicCreateRequest(
        @NotNull Integer categoryId,
        @NotBlank String title,
        String description,
        @NotNull LocalDateTime voteStartAt,
        @NotNull LocalDateTime voteDeadlineAt
) {
}

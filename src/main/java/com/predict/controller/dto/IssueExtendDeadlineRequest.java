package com.predict.controller.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record IssueExtendDeadlineRequest(@NotNull LocalDateTime newDeadline) {
}

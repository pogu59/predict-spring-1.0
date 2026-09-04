package com.predict.controller.dto;

import jakarta.validation.constraints.NotNull;

public record IssueConfirmRequest(@NotNull Long correctOptionId) {
}

package com.predict.controller.dto;

import jakarta.validation.constraints.NotNull;

public record VoteRequest(@NotNull Long userId, @NotNull Long optionId) {
}

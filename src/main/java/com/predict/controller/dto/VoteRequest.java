package com.predict.controller.dto;

import com.predict.enums.Choice;
import jakarta.validation.constraints.NotNull;

public record VoteRequest(@NotNull Long userId, @NotNull Choice choice) {
}

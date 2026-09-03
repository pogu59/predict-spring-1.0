package com.predict.controller.dto;

import jakarta.validation.constraints.NotNull;

public record TopicConfirmRequest(@NotNull Long correctOptionId) {
}

package com.predict.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record ShareClickRequest(@NotBlank String channel) {
}

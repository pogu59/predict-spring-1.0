package com.predict.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
        @NotBlank String nickname,
        String signupChannel,
        String referralCode
) {
}

package com.predict.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReplyCreateRequest(
        @NotBlank @Size(max = 1000) String content
) {
}

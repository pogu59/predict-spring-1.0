package com.predict.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** stake: 이 투표에 걸 신용도. 최소 1, 보유 잔액 이하만 허용된다(VoteService에서 검증). */
public record VoteRequest(@NotNull Long userId, @NotNull Long optionId, @NotNull @Min(1) Integer stake) {
}

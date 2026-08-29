package com.predict.controller.dto;

import com.predict.ShareClick;

import java.time.LocalDateTime;

public record ShareClickResponse(Long id, String referralCode, String channel, LocalDateTime clickedAt) {
    public static ShareClickResponse from(ShareClick shareClick) {
        return new ShareClickResponse(shareClick.getId(), shareClick.getReferralCode(),
                shareClick.getChannel(), shareClick.getClickedAt());
    }
}

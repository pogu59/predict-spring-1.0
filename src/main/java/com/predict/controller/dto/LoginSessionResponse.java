package com.predict.controller.dto;

import com.predict.LoginSession;

import java.time.LocalDateTime;

public record LoginSessionResponse(Long id, Long userId, LocalDateTime loginAt) {
    public static LoginSessionResponse from(LoginSession loginSession) {
        return new LoginSessionResponse(loginSession.getId(), loginSession.getUser().getId(), loginSession.getLoginAt());
    }
}

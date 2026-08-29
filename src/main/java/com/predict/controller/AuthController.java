package com.predict.controller;

import com.predict.controller.dto.MeResponse;
import com.predict.service.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인 후 프론트가 들고 있는 세션 토큰(Authorization: Bearer {token})으로
 * 현재 로그인된 유저를 조회하는 용도. login_sessions.session_token을 그대로 인증 토큰으로 쓴다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CurrentUserService currentUserService;

    public AuthController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/me")
    public MeResponse me(@RequestHeader("Authorization") String authorizationHeader) {
        return MeResponse.from(currentUserService.requireUser(authorizationHeader));
    }
}

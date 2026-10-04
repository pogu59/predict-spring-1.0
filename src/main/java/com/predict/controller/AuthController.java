package com.predict.controller;

import com.predict.User;
import com.predict.controller.dto.CommunityRequests.EmailLoginRequest;
import com.predict.controller.dto.CommunityRequests.EmailSignupRequest;
import com.predict.controller.dto.CommunityRequests.NicknameCheckResponse;
import com.predict.controller.dto.CommunityRequests.SocialSignupRequest;
import com.predict.controller.dto.CommunityRequests.TokenResponse;
import com.predict.controller.dto.MeResponse;
import com.predict.service.AuthService;
import com.predict.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인 후 프론트가 들고 있는 세션 토큰(Authorization: Bearer {token})으로 현재 유저를 조회하고,
 * 이메일 가입/로그인·닉네임 확인·소셜 가입 마무리를 처리한다. 토큰은 login_sessions.session_token이다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CurrentUserService currentUserService;
    private final AuthService authService;

    public AuthController(CurrentUserService currentUserService, AuthService authService) {
        this.currentUserService = currentUserService;
        this.authService = authService;
    }

    @GetMapping("/me")
    public MeResponse me(@RequestHeader("Authorization") String authorizationHeader) {
        return MeResponse.from(currentUserService.requireUser(authorizationHeader));
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse signup(@Valid @RequestBody EmailSignupRequest request) {
        return new TokenResponse(authService.signupEmail(request.email(), request.password(), request.nickname(),
                request.terms()));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody EmailLoginRequest request) {
        return new TokenResponse(authService.loginEmail(request.email(), request.password()));
    }

    @GetMapping("/nickname-check")
    public NicknameCheckResponse checkNickname(@RequestParam String nickname) {
        return new NicknameCheckResponse(authService.isNicknameAvailable(nickname.trim()));
    }

    /** 소셜 로그인으로 자동 생성된 계정의 닉네임·약관 단계. */
    @PostMapping("/social-signup")
    public MeResponse completeSocialSignup(@RequestHeader("Authorization") String authorization,
                                           @Valid @RequestBody SocialSignupRequest request) {
        User user = currentUserService.requireUser(authorization);
        return MeResponse.from(authService.completeSocialSignup(user, request.nickname().trim(), request.terms()));
    }
}

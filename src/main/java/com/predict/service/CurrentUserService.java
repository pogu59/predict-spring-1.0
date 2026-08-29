package com.predict.service;

import com.predict.LoginSession;
import com.predict.User;
import com.predict.enums.Role;
import com.predict.repository.LoginSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Authorization: Bearer {login_sessions.session_token} 헤더로 현재 유저를 식별한다.
 * 이 프로젝트는 별도 인가 필터가 없어(SecurityConfig 참고) 각 API가 필요할 때 직접
 * 호출해서 쓰는 구조이며, 그 파싱 로직을 한 곳에 모아둔 것이 이 클래스다.
 */
@Service
public class CurrentUserService {

    private final LoginSessionRepository loginSessionRepository;

    public CurrentUserService(LoginSessionRepository loginSessionRepository) {
        this.loginSessionRepository = loginSessionRepository;
    }

    public User requireUser(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        LoginSession loginSession = loginSessionRepository.findBySessionToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 세션입니다"));
        return loginSession.getUser();
    }

    /** 관리자 페이지 API 전용. 로그인은 했지만 관리자가 아니면 403. */
    public User requireAdmin(String authorizationHeader) {
        User user = requireUser(authorizationHeader);
        if (user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다");
        }
        return user;
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization 헤더가 필요합니다");
        }
        return authorizationHeader.substring("Bearer ".length());
    }
}

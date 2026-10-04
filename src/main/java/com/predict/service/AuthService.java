package com.predict.service;

import com.predict.User;
import com.predict.controller.dto.CommunityRequests.Terms;
import com.predict.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 이메일 가입/로그인, 닉네임 중복 확인, 소셜 가입 마무리. 로그인 성공 시 발급하는 토큰은
 * 카카오 로그인과 같은 login_sessions.session_token이다(LoginSessionService).
 * 검증 규칙과 메시지는 프론트 가입 화면(lib/validation.ts)과 같다.
 */
@Service
public class AuthService {

    private static final Pattern NICKNAME = Pattern.compile("^[가-힣a-zA-Z0-9_]{2,10}$");
    private static final Pattern PASSWORD_LETTER = Pattern.compile("[A-Za-z]");
    private static final Pattern PASSWORD_DIGIT = Pattern.compile("\\d");
    private static final List<String> RESERVED_NICKNAMES = List.of("admin", "predict", "관리자", "운영자");

    private final UserRepository userRepository;
    private final LoginSessionService loginSessionService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, LoginSessionService loginSessionService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.loginSessionService = loginSessionService;
        this.passwordEncoder = passwordEncoder;
    }

    /** 이메일 가입 — 신규 유저는 신용도 500으로 시작한다. 성공하면 바로 로그인 토큰을 돌려준다. */
    @Transactional
    public String signupEmail(String email, String password, String nickname, Terms terms) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (!terms.requiredAgreed()) {
            throw new IllegalArgumentException("필수 약관에 동의해 주세요");
        }
        if (password.length() < 8 || !PASSWORD_LETTER.matcher(password).find() || !PASSWORD_DIGIT.matcher(password).find()) {
            throw new IllegalArgumentException("영문과 숫자를 포함해 8자 이상 입력해 주세요");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("이미 가입된 이메일이에요");
        }
        requireAvailableNickname(nickname, null);
        User user = userRepository.save(User.forEmail(nickname, normalizedEmail, passwordEncoder.encode(password),
                terms.marketing()));
        return loginSessionService.recordLogin(user).getSessionToken();
    }

    @Transactional
    public String loginEmail(String email, String password) {
        User user = userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .filter(u -> u.getPasswordHash() != null && passwordEncoder.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호를 확인해 주세요."));
        return loginSessionService.recordLogin(user).getSessionToken();
    }

    public boolean isNicknameAvailable(String nickname) {
        return NICKNAME.matcher(nickname).matches()
                && !RESERVED_NICKNAMES.contains(nickname.toLowerCase(Locale.ROOT))
                && !userRepository.existsByNicknameIgnoreCase(nickname);
    }

    /** 소셜 로그인으로 자동 생성된 유저의 닉네임·약관 단계를 마무리한다. 지금 쓰는 닉네임을 그대로 둬도 된다. */
    @Transactional
    public User completeSocialSignup(User user, String nickname, Terms terms) {
        if (!terms.requiredAgreed()) {
            throw new IllegalArgumentException("필수 약관에 동의해 주세요");
        }
        requireAvailableNickname(nickname, user);
        user.completeProfile(nickname, terms.marketing());
        return user;
    }

    private void requireAvailableNickname(String nickname, User self) {
        if (self != null && self.getNickname().equalsIgnoreCase(nickname)) {
            return;
        }
        if (!NICKNAME.matcher(nickname).matches()) {
            throw new IllegalArgumentException("2~10자의 한글, 영문, 숫자만 쓸 수 있어요");
        }
        if (!isNicknameAvailable(nickname)) {
            throw new IllegalStateException("이미 사용 중인 닉네임이에요");
        }
    }
}

package com.predict.config;

import com.predict.LoginSession;
import com.predict.User;
import com.predict.service.LoginSessionService;
import com.predict.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

/**
 * 카카오 로그인 성공 시 우리 서비스의 유저로 매핑하고, 프론트가 이후 API 호출에 쓸
 * 세션 토큰을 발급해 프론트 콜백 페이지로 리다이렉트한다.
 */
@Component
public class KakaoOAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final LoginSessionService loginSessionService;
    private final String frontendUrl;

    public KakaoOAuth2LoginSuccessHandler(UserService userService,
                                           LoginSessionService loginSessionService,
                                           @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl) {
        this.userService = userService;
        this.loginSessionService = loginSessionService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String kakaoId = String.valueOf(oAuth2User.getAttributes().get("id"));
        String nickname = extractNickname(oAuth2User.getAttributes());

        User user = userService.findOrCreateByKakao(kakaoId, nickname);
        LoginSession loginSession = loginSessionService.recordLogin(user);

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/auth/callback")
                .queryParam("token", loginSession.getSessionToken())
                .build()
                .toUriString();
        response.sendRedirect(redirectUrl);
    }

    @SuppressWarnings("unchecked")
    private String extractNickname(Map<String, Object> attributes) {
        Object kakaoAccount = attributes.get("kakao_account");
        if (kakaoAccount instanceof Map<?, ?> accountMap) {
            Object profile = accountMap.get("profile");
            if (profile instanceof Map<?, ?> profileMap && profileMap.get("nickname") != null) {
                return String.valueOf(profileMap.get("nickname"));
            }
        }
        Object properties = attributes.get("properties");
        if (properties instanceof Map<?, ?> propertiesMap && propertiesMap.get("nickname") != null) {
            return String.valueOf(propertiesMap.get("nickname"));
        }
        return "카카오유저";
    }
}

package com.predict.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 임시 보안 설정. 이 프로젝트에는 아직 자격증명 기반 인증 체계가 없어(docs/predict.md 참고,
 * "지인 베타" 단계) API 자체는 전체 개방하되, 카카오 소셜 로그인만 붙여둔다.
 * 로그인 후 발급되는 토큰(login_sessions.session_token)으로 유저를 식별하는 것은
 * 각 API가 필요에 따라 직접 처리한다(별도 인가 필터 없음).
 */
@Configuration
public class SecurityConfig {

    private final KakaoOAuth2LoginSuccessHandler kakaoOAuth2LoginSuccessHandler;

    public SecurityConfig(KakaoOAuth2LoginSuccessHandler kakaoOAuth2LoginSuccessHandler) {
        this.kakaoOAuth2LoginSuccessHandler = kakaoOAuth2LoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2.successHandler(kakaoOAuth2LoginSuccessHandler));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.frontend-url:http://localhost:3000}") String frontendUrl) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendUrl));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

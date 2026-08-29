package com.predict.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 임시 보안 설정. 이 프로젝트에는 아직 인증(로그인/자격증명) 체계가 없어(docs/predict.md 참고,
 * "지인 베타" 단계) spring-boot-starter-security의 기본 자동설정(모든 요청 로그인 요구)을 끄고
 * 전체 개방한다. 실제 인증을 도입하면 이 설정을 교체해야 한다.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}

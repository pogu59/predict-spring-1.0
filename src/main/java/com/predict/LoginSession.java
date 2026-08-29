package com.predict;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 로그인 기록. sessionToken은 로그인 이벤트 로그이자 동시에 프론트가 이후 요청에
 * 실어 보내는 인증 토큰으로 재사용된다(Bearer 토큰 방식의 별도 세션 테이블 없이).
 */
@Entity
@Table(name = "login_sessions")
public class LoginSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "session_token", nullable = false, unique = true, length = 36)
    private String sessionToken;

    @CreationTimestamp
    @Column(name = "login_at", nullable = false, updatable = false)
    private LocalDateTime loginAt;

    protected LoginSession() {
    }

    public LoginSession(User user) {
        this.user = user;
        this.sessionToken = UUID.randomUUID().toString();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public LocalDateTime getLoginAt() {
        return loginAt;
    }
}

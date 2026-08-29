package com.predict.repository;

import com.predict.LoginSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoginSessionRepository extends JpaRepository<LoginSession, Long> {

    Optional<LoginSession> findBySessionToken(String sessionToken);
}

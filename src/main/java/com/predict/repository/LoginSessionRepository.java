package com.predict.repository;

import com.predict.LoginSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginSessionRepository extends JpaRepository<LoginSession, Long> {
}

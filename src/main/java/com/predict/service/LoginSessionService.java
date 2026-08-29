package com.predict.service;

import com.predict.LoginSession;
import com.predict.User;
import com.predict.repository.LoginSessionRepository;
import org.springframework.stereotype.Service;

@Service
public class LoginSessionService {

    private final LoginSessionRepository loginSessionRepository;

    public LoginSessionService(LoginSessionRepository loginSessionRepository) {
        this.loginSessionRepository = loginSessionRepository;
    }

    public LoginSession recordLogin(User user) {
        return loginSessionRepository.save(new LoginSession(user));
    }
}

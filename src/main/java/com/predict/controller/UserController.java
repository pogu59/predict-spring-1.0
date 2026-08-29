package com.predict.controller;

import com.predict.User;
import com.predict.controller.dto.LoginSessionResponse;
import com.predict.controller.dto.ShareClickRequest;
import com.predict.controller.dto.ShareClickResponse;
import com.predict.controller.dto.SignupRequest;
import com.predict.controller.dto.UserResponse;
import com.predict.repository.UserRepository;
import com.predict.service.LoginSessionService;
import com.predict.service.ShareClickService;
import com.predict.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final LoginSessionService loginSessionService;
    private final ShareClickService shareClickService;

    public UserController(UserRepository userRepository, UserService userService,
                           LoginSessionService loginSessionService, ShareClickService shareClickService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.loginSessionService = loginSessionService;
        this.shareClickService = shareClickService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signup(@Valid @RequestBody SignupRequest request) {
        User user = userService.signup(request.nickname(), request.signupChannel(), request.referralCode());
        return UserResponse.from(user);
    }

    @GetMapping("/{userId}")
    public UserResponse get(@PathVariable Long userId) {
        return UserResponse.from(requireUser(userId));
    }

    @PostMapping("/{userId}/login-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginSessionResponse recordLogin(@PathVariable Long userId) {
        return LoginSessionResponse.from(loginSessionService.recordLogin(requireUser(userId)));
    }

    @PostMapping("/{userId}/share-clicks")
    @ResponseStatus(HttpStatus.CREATED)
    public ShareClickResponse recordShareClick(@PathVariable Long userId, @Valid @RequestBody ShareClickRequest request) {
        return ShareClickResponse.from(shareClickService.recordClick(requireUser(userId), request.channel()));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저: " + userId));
    }
}

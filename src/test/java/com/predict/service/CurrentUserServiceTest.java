package com.predict.service;

import com.predict.LoginSession;
import com.predict.User;
import com.predict.enums.Role;
import com.predict.repository.LoginSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private LoginSessionRepository loginSessionRepository;

    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        currentUserService = new CurrentUserService(loginSessionRepository);
    }

    @Test
    void requireUser_missingHeader_throws401() {
        assertThatThrownBy(() -> currentUserService.requireUser(null))
                .hasMessageContaining("Authorization");
    }

    @Test
    void requireUser_unknownToken_throws401() {
        when(loginSessionRepository.findBySessionToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> currentUserService.requireUser("Bearer bad-token"))
                .hasMessageContaining("유효하지 않은");
    }

    @Test
    void requireAdmin_nonAdminUser_throws403() throws Exception {
        User user = new User("일반유저", "direct", null);
        LoginSession session = new LoginSession(user);
        when(loginSessionRepository.findBySessionToken("token")).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> currentUserService.requireAdmin("Bearer token"))
                .hasMessageContaining("관리자 권한");
    }

    @Test
    void requireAdmin_adminUser_returnsUser() throws Exception {
        User user = new User("관리자", "direct", null);
        setRole(user, Role.ADMIN);
        LoginSession session = new LoginSession(user);
        when(loginSessionRepository.findBySessionToken("token")).thenReturn(Optional.of(session));

        assertThat(currentUserService.requireAdmin("Bearer token")).isEqualTo(user);
    }

    private void setRole(User user, Role role) throws Exception {
        Field field = User.class.getDeclaredField("role");
        field.setAccessible(true);
        field.set(user, role);
    }
}

package com.predict.controller;

import com.predict.User;
import com.predict.controller.dto.SignupRequest;
import com.predict.controller.dto.UserResponse;
import com.predict.repository.ScoreSettlementRepository;
import com.predict.repository.UserRepository;
import com.predict.repository.VoteRepository;
import com.predict.service.LoginSessionService;
import com.predict.service.ShareClickService;
import com.predict.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @Mock
    private LoginSessionService loginSessionService;
    @Mock
    private ShareClickService shareClickService;
    @Mock
    private VoteRepository voteRepository;
    @Mock
    private ScoreSettlementRepository scoreSettlementRepository;

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController(userRepository, userService, loginSessionService, shareClickService,
                voteRepository, scoreSettlementRepository);
    }

    @Test
    void signup_delegatesToServiceAndReturnsResponse() {
        User user = new User("닉네임", "direct", null);
        when(userService.signup("닉네임", "direct", null)).thenReturn(user);

        UserResponse response = userController.signup(new SignupRequest("닉네임", "direct", null));

        assertThat(response.nickname()).isEqualTo("닉네임");
        assertThat(response.credibilityScore()).isEqualTo(User.STARTING_CREDIBILITY_SCORE);
    }

    @Test
    void signup_invalidReferralCode_propagatesAsIllegalArgument() {
        when(userService.signup("닉네임", "direct", "BADCODE"))
                .thenThrow(new IllegalArgumentException("존재하지 않는 추천 코드: BADCODE"));

        assertThatThrownBy(() -> userController.signup(new SignupRequest("닉네임", "direct", "BADCODE")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void get_unknownUser_throwsIllegalArgument() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userController.get(99L)).isInstanceOf(IllegalArgumentException.class);
    }
}

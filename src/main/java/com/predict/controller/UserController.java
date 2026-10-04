package com.predict.controller;

import com.predict.User;
import com.predict.Vote;
import com.predict.controller.dto.LoginSessionResponse;
import com.predict.controller.dto.MyStatsResponse;
import com.predict.controller.dto.MyVoteResponse;
import com.predict.controller.dto.ShareClickRequest;
import com.predict.controller.dto.ShareClickResponse;
import com.predict.controller.dto.SignupRequest;
import com.predict.controller.dto.UserResponse;
import com.predict.enums.SettlementResult;
import com.predict.repository.ScoreSettlementRepository;
import com.predict.repository.UserRepository;
import com.predict.repository.VoteRepository;
import com.predict.service.LoginSessionService;
import com.predict.service.ShareClickService;
import com.predict.service.UserService;
import com.predict.service.VoteCountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final LoginSessionService loginSessionService;
    private final ShareClickService shareClickService;
    private final VoteRepository voteRepository;
    private final ScoreSettlementRepository scoreSettlementRepository;
    private final VoteCountService voteCountService;

    public UserController(UserRepository userRepository, UserService userService,
                           LoginSessionService loginSessionService, ShareClickService shareClickService,
                           VoteRepository voteRepository, ScoreSettlementRepository scoreSettlementRepository,
                           VoteCountService voteCountService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.loginSessionService = loginSessionService;
        this.shareClickService = shareClickService;
        this.voteRepository = voteRepository;
        this.scoreSettlementRepository = scoreSettlementRepository;
        this.voteCountService = voteCountService;
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

    /** 마이페이지 요약 통계. */
    @GetMapping("/{userId}/stats")
    public MyStatsResponse stats(@PathVariable Long userId) {
        requireUser(userId);
        long totalVotes = voteRepository.countByUserIdAndIssueDeletedFalse(userId);
        long correctCount = scoreSettlementRepository.countByUserIdAndResultAndIsReversedFalse(userId, SettlementResult.CORRECT);
        long gradedCount = scoreSettlementRepository.countByUserIdAndResultInAndIsReversedFalse(
                userId, List.of(SettlementResult.CORRECT, SettlementResult.INCORRECT));
        return new MyStatsResponse(totalVotes, correctCount, gradedCount);
    }

    /** 마이페이지 최근 투표 기록(최신순). 삭제된 이슈(베팅액 환불 완료)의 투표는 뺀다. */
    @GetMapping("/{userId}/votes")
    public List<MyVoteResponse> votes(@PathVariable Long userId) {
        requireUser(userId);
        List<Vote> votes = voteRepository.findByUserIdAndIssueDeletedFalseOrderByVotedAtDesc(userId);
        Map<Long, Integer> counts = voteCountService.countsByOption(
                votes.stream().map(Vote::getIssue).distinct().toList());
        return votes.stream()
                .map(vote -> MyVoteResponse.from(vote,
                        scoreSettlementRepository.findByVoteIdAndIsReversedFalse(vote.getId()), counts))
                .toList();
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저: " + userId));
    }
}

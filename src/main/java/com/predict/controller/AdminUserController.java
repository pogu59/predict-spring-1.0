package com.predict.controller;

import com.predict.User;
import com.predict.controller.dto.AdminUserDetailResponse;
import com.predict.controller.dto.AdminUserListItemResponse;
import com.predict.controller.dto.PageResponse;
import com.predict.enums.Role;
import com.predict.enums.SettlementResult;
import com.predict.enums.Tier;
import com.predict.repository.ScoreSettlementRepository;
import com.predict.repository.UserRepository;
import com.predict.repository.VoteRepository;
import com.predict.service.CurrentUserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 관리자 페이지 — 유저 검색/상세 (docs/predict.md 5장). 권한 부여/해제 API는 없다 —
 * role은 DB에서 직접 수정한다.
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final VoteRepository voteRepository;
    private final ScoreSettlementRepository scoreSettlementRepository;
    private final CurrentUserService currentUserService;

    public AdminUserController(UserRepository userRepository, VoteRepository voteRepository,
                                ScoreSettlementRepository scoreSettlementRepository,
                                CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.voteRepository = voteRepository;
        this.scoreSettlementRepository = scoreSettlementRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public PageResponse<AdminUserListItemResponse> list(@RequestHeader("Authorization") String authorization,
                                                          @RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) Role role,
                                                          @RequestParam(required = false) Tier tier,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        currentUserService.requireAdmin(authorization);
        var result = userRepository.search(keyword, role, tier, PageRequest.of(page, size));
        return PageResponse.from(result, AdminUserListItemResponse::from);
    }

    @GetMapping("/{userId}")
    public AdminUserDetailResponse get(@RequestHeader("Authorization") String authorization,
                                        @PathVariable Long userId) {
        currentUserService.requireAdmin(authorization);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저: " + userId));

        long totalVotes = voteRepository.countByUserId(userId);
        long correctCount = scoreSettlementRepository.countByUserIdAndResultAndIsReversedFalse(userId, SettlementResult.CORRECT);
        long gradedCount = scoreSettlementRepository.countByUserIdAndResultInAndIsReversedFalse(
                userId, List.of(SettlementResult.CORRECT, SettlementResult.INCORRECT));

        return AdminUserDetailResponse.from(user, totalVotes, correctCount, gradedCount);
    }
}

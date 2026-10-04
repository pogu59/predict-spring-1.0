package com.predict.controller;

import com.predict.User;
import com.predict.controller.dto.AdminUserDetailResponse;
import com.predict.controller.dto.AdminUserListItemResponse;
import com.predict.controller.dto.CommunityRequests.RoleRequest;
import com.predict.controller.dto.CommunityRequests.SuspensionRequest;
import com.predict.controller.dto.PageResponse;
import com.predict.enums.Role;
import com.predict.enums.SettlementResult;
import com.predict.enums.Tier;
import com.predict.repository.ScoreSettlementRepository;
import com.predict.repository.UserRepository;
import com.predict.repository.VoteRepository;
import com.predict.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 관리자 페이지 — 유저 검색/상세, 관리자 지정·해제, 활동 정지·해제 (docs/predict.md 5장). */
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
        var result = userRepository.search(keyword, role, tier, PageRequest.of(page, Math.min(size, 500)));
        return PageResponse.from(result, AdminUserListItemResponse::from);
    }

    @GetMapping("/{userId}")
    public AdminUserDetailResponse get(@RequestHeader("Authorization") String authorization,
                                        @PathVariable Long userId) {
        currentUserService.requireAdmin(authorization);
        User user = requireUser(userId);

        long totalVotes = voteRepository.countByUserIdAndIssueDeletedFalse(userId);
        long correctCount = scoreSettlementRepository.countByUserIdAndResultAndIsReversedFalse(userId, SettlementResult.CORRECT);
        long gradedCount = scoreSettlementRepository.countByUserIdAndResultInAndIsReversedFalse(
                userId, List.of(SettlementResult.CORRECT, SettlementResult.INCORRECT));

        return AdminUserDetailResponse.from(user, totalVotes, correctCount, gradedCount);
    }

    /** 관리자 지정/해제. 자기 자신의 권한은 바꿀 수 없다(마지막 관리자가 스스로 잠기는 사고 방지). */
    @PatchMapping("/{userId}/role")
    @Transactional
    public AdminUserListItemResponse setRole(@RequestHeader("Authorization") String authorization,
                                             @PathVariable Long userId, @Valid @RequestBody RoleRequest request) {
        User admin = currentUserService.requireAdmin(authorization);
        if (admin.getId().equals(userId)) {
            throw new IllegalStateException("자기 자신의 권한은 바꿀 수 없어요");
        }
        User user = requireUser(userId);
        user.changeRole(request.role());
        return AdminUserListItemResponse.from(user);
    }

    /** 활동 정지/해제 — 정지된 유저는 투표·댓글·글쓰기를 할 수 없다. */
    @PatchMapping("/{userId}/suspension")
    @Transactional
    public AdminUserListItemResponse setSuspended(@RequestHeader("Authorization") String authorization,
                                                  @PathVariable Long userId,
                                                  @RequestBody SuspensionRequest request) {
        User admin = currentUserService.requireAdmin(authorization);
        if (admin.getId().equals(userId)) {
            throw new IllegalStateException("자기 자신은 정지할 수 없어요");
        }
        User user = requireUser(userId);
        user.setSuspended(request.suspended());
        return AdminUserListItemResponse.from(user);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저: " + userId));
    }
}

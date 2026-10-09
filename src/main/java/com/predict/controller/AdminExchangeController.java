package com.predict.controller;

import com.predict.RewardExchangeRequest;
import com.predict.User;
import com.predict.controller.dto.RewardDtos.AdminExchangeResponse;
import com.predict.controller.dto.RewardDtos.ExchangeRejectRequest;
import com.predict.enums.ExchangeStatus;
import com.predict.enums.SubmissionStatus;
import com.predict.repository.MissionSubmissionRepository;
import com.predict.service.CurrentUserService;
import com.predict.service.RewardService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 관리자 교환 승인. 기프티콘은 운영자가 직접 보낸 뒤 "발송 완료"로 처리한다(MVP는 수동 발송).
 * 반려하면 포인트가 환불 기록과 함께 바로 돌아간다.
 */
@RestController
@RequestMapping("/api/admin/exchanges")
public class AdminExchangeController {

    private final RewardService rewardService;
    private final MissionSubmissionRepository submissionRepository;
    private final CurrentUserService currentUserService;

    public AdminExchangeController(RewardService rewardService, MissionSubmissionRepository submissionRepository,
                                   CurrentUserService currentUserService) {
        this.rewardService = rewardService;
        this.submissionRepository = submissionRepository;
        this.currentUserService = currentUserService;
    }

    /** status를 주면 그 상태만(확인 대기는 오래된 순), 없으면 전체 최신순. */
    @GetMapping
    public List<AdminExchangeResponse> list(@RequestHeader("Authorization") String authorization,
                                            @RequestParam(required = false) ExchangeStatus status) {
        currentUserService.requireAdmin(authorization);
        return rewardService.exchangesFor(status).stream().map(this::toResponse).toList();
    }

    @PostMapping("/{exchangeId}/send")
    public AdminExchangeResponse markSent(@RequestHeader("Authorization") String authorization,
                                          @PathVariable Long exchangeId) {
        User admin = currentUserService.requireAdmin(authorization);
        return toResponse(rewardService.markSent(admin, exchangeId));
    }

    @PostMapping("/{exchangeId}/reject")
    public AdminExchangeResponse reject(@RequestHeader("Authorization") String authorization,
                                        @PathVariable Long exchangeId,
                                        @Valid @RequestBody ExchangeRejectRequest request) {
        User admin = currentUserService.requireAdmin(authorization);
        return toResponse(rewardService.rejectExchange(admin, exchangeId, request.reason()));
    }

    private AdminExchangeResponse toResponse(RewardExchangeRequest request) {
        Long userId = request.getUser().getId();
        return AdminExchangeResponse.of(request,
                submissionRepository.countByUserIdAndStatus(userId, SubmissionStatus.APPROVED),
                submissionRepository.countByUserIdAndStatus(userId, SubmissionStatus.REJECTED));
    }
}

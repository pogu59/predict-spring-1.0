package com.predict.controller;

import com.predict.User;
import com.predict.controller.dto.RewardDtos.ExchangeCreateRequest;
import com.predict.controller.dto.RewardDtos.ExchangeResponse;
import com.predict.controller.dto.RewardDtos.WalletResponse;
import com.predict.reward.RewardCatalog;
import com.predict.service.CurrentUserService;
import com.predict.service.RewardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내 리워드 포인트 지갑과 기프티콘 교환. 로그인 토큰의 유저 것만 다룬다(다른 사람 지갑은 볼 수 없음).
 */
@RestController
@RequestMapping("/api/reward")
public class RewardController {

    private final RewardService rewardService;
    private final CurrentUserService currentUserService;

    public RewardController(RewardService rewardService, CurrentUserService currentUserService) {
        this.rewardService = rewardService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/me")
    public WalletResponse me(@RequestHeader("Authorization") String authorization) {
        User user = currentUserService.requireUser(authorization);
        return WalletResponse.of(rewardService.summary(user), RewardCatalog.PRODUCTS);
    }

    @PostMapping("/exchanges")
    @ResponseStatus(HttpStatus.CREATED)
    public ExchangeResponse requestExchange(@RequestHeader("Authorization") String authorization,
                                            @Valid @RequestBody ExchangeCreateRequest request) {
        User user = currentUserService.requireActiveUser(authorization);
        return ExchangeResponse.from(rewardService.requestExchange(user, request.productCode()));
    }

    /** 운영자가 확인하기 전(확인 중)에만 취소할 수 있다. 포인트는 바로 돌아온다. */
    @PostMapping("/exchanges/{exchangeId}/cancel")
    public ExchangeResponse cancel(@RequestHeader("Authorization") String authorization,
                                   @PathVariable Long exchangeId) {
        User user = currentUserService.requireUser(authorization);
        return ExchangeResponse.from(rewardService.cancelExchange(user, exchangeId));
    }
}

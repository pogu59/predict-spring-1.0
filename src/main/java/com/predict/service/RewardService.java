package com.predict.service;

import com.predict.MissionSubmission;
import com.predict.RewardExchangeRequest;
import com.predict.RewardTransaction;
import com.predict.RewardWallet;
import com.predict.User;
import com.predict.enums.ExchangeStatus;
import com.predict.enums.RewardTransactionType;
import com.predict.repository.RewardExchangeRequestRepository;
import com.predict.repository.RewardTransactionRepository;
import com.predict.repository.RewardWalletRepository;
import com.predict.reward.RewardCatalog;
import com.predict.reward.RewardProduct;
import com.predict.reward.RewardSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 리워드 포인트 적립과 기프티콘 교환. 신용도(User.credibilityScore)는 절대 건드리지 않는다 —
 * 신용도와 포인트 사이에는 전환 경로가 없다(기획서 "신용도와 리워드 포인트").
 * 잔액은 항상 RewardWallet을 통해서만 바뀌고, 모든 변동은 같은 트랜잭션에서 원장
 * (reward_transactions)에 남긴다. 같은 사건은 idempotency key로 한 번만 기록된다.
 */
@Service
public class RewardService {

    static final List<RewardTransactionType> EARNING_TYPES =
            List.of(RewardTransactionType.EARN, RewardTransactionType.BONUS);

    private final RewardWalletRepository walletRepository;
    private final RewardTransactionRepository transactionRepository;
    private final RewardExchangeRequestRepository exchangeRepository;

    public RewardService(RewardWalletRepository walletRepository,
                         RewardTransactionRepository transactionRepository,
                         RewardExchangeRequestRepository exchangeRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.exchangeRepository = exchangeRepository;
    }

    @Transactional(readOnly = true)
    public int balanceOf(User user) {
        return walletRepository.findById(user.getId()).map(RewardWallet::getBalance).orElse(0);
    }

    /**
     * 포인트를 적립하고 원장에 남긴다. 같은 idempotencyKey가 이미 있으면 아무것도 하지 않고 null을
     * 돌려준다(재시도·중복 호출에도 한 번만 적립).
     */
    @Transactional
    public RewardTransaction credit(User user, RewardTransactionType type, int amount, String idempotencyKey,
                                    MissionSubmission submission, RewardExchangeRequest exchange, String memo) {
        if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            return null;
        }
        int balanceAfter = walletOf(user).earn(amount);
        return transactionRepository.save(new RewardTransaction(
                user, type, amount, balanceAfter, idempotencyKey, submission, exchange, memo));
    }

    @Transactional(readOnly = true)
    public RewardSummary summary(User user) {
        List<RewardExchangeRequest> exchanges = exchangeRepository.findByUserIdOrderByIdDesc(user.getId());
        int pending = exchanges.stream()
                .filter(exchange -> exchange.getStatus() == ExchangeStatus.REQUESTED)
                .mapToInt(RewardExchangeRequest::getPoints)
                .sum();
        long monthEarned = transactionRepository.sumAmountSince(
                user.getId(), EARNING_TYPES, LocalDate.now().withDayOfMonth(1).atStartOfDay());
        return new RewardSummary(balanceOf(user), pending, (int) monthEarned,
                transactionRepository.findTop30ByUserIdOrderByIdDesc(user.getId()), exchanges);
    }

    /** 교환 신청 — 포인트를 바로 차감해 두고(에스크로) 운영자 확인을 기다린다. */
    @Transactional
    public RewardExchangeRequest requestExchange(User user, String productCode) {
        CurrentUserService.requireNotSuspended(user);
        RewardProduct product = RewardCatalog.require(productCode);
        int balanceAfter = walletOf(user).spend(product.points());
        RewardExchangeRequest request = exchangeRepository.save(new RewardExchangeRequest(user, product));
        transactionRepository.save(new RewardTransaction(
                user, RewardTransactionType.EXCHANGE, -product.points(), balanceAfter,
                "exchange:" + request.getId(), null, request, product.name() + " 교환 신청"));
        return request;
    }

    /** 신청자 취소 — 운영자가 확인하기 전에만. 포인트는 환불 기록과 함께 돌아간다. */
    @Transactional
    public RewardExchangeRequest cancelExchange(User user, Long exchangeId) {
        RewardExchangeRequest request = requireExchange(exchangeId);
        request.cancelBy(user);
        refund(request, "교환 취소 · " + request.getProductName());
        return request;
    }

    /** 운영자가 기프티콘을 보낸 뒤 발송 완료로 처리. */
    @Transactional
    public RewardExchangeRequest markSent(User admin, Long exchangeId) {
        RewardExchangeRequest request = requireExchange(exchangeId);
        request.markSent(admin);
        return request;
    }

    /** 운영자 반려 — 포인트는 환불 기록과 함께 바로 돌아간다. */
    @Transactional
    public RewardExchangeRequest rejectExchange(User admin, Long exchangeId, String reason) {
        RewardExchangeRequest request = requireExchange(exchangeId);
        request.reject(admin, reason);
        refund(request, "교환 반려 · " + request.getRejectReason());
        return request;
    }

    @Transactional(readOnly = true)
    public List<RewardExchangeRequest> exchangesFor(ExchangeStatus status) {
        return status == null
                ? exchangeRepository.findAllByOrderByIdDesc()
                : exchangeRepository.findByStatusOrderByIdAsc(status);
    }

    /** 지갑이 없으면 0P 지갑을 만든다. 쓰기 트랜잭션 안에서만 부른다. */
    private RewardWallet walletOf(User user) {
        return walletRepository.findById(user.getId())
                .orElseGet(() -> walletRepository.save(new RewardWallet(user.getId())));
    }

    private RewardExchangeRequest requireExchange(Long exchangeId) {
        return exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 교환 신청: " + exchangeId));
    }

    private void refund(RewardExchangeRequest request, String memo) {
        credit(request.getUser(), RewardTransactionType.REFUND, request.getPoints(),
                "refund:" + request.getId(), null, request, memo);
    }
}

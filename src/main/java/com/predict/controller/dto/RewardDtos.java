package com.predict.controller.dto;

import com.predict.RewardExchangeRequest;
import com.predict.RewardTransaction;
import com.predict.enums.ExchangeStatus;
import com.predict.enums.RewardTransactionType;
import com.predict.reward.RewardProduct;
import com.predict.reward.RewardSummary;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/** 리워드 포인트(지갑·교환) 요청/응답 DTO 모음. 신용도 값은 여기에 절대 섞지 않는다. */
public final class RewardDtos {

    private RewardDtos() {
    }

    public record ExchangeCreateRequest(@NotBlank @Size(max = 50) String productCode) {
    }

    public record ExchangeRejectRequest(@NotBlank @Size(max = 200) String reason) {
    }

    /** amount는 부호 있는 변동량(+적립, -교환 신청). */
    public record TransactionResponse(Long id, RewardTransactionType type, int amount, int balanceAfter,
                                      String memo, LocalDateTime createdAt) {

        public static TransactionResponse from(RewardTransaction transaction) {
            return new TransactionResponse(transaction.getId(), transaction.getType(), transaction.getAmount(),
                    transaction.getBalanceAfter(), transaction.getMemo(), transaction.getCreatedAt());
        }
    }

    public record ExchangeResponse(Long id, String productCode, String productName, int points,
                                   ExchangeStatus status, String rejectReason,
                                   LocalDateTime createdAt, LocalDateTime handledAt) {

        public static ExchangeResponse from(RewardExchangeRequest request) {
            return new ExchangeResponse(request.getId(), request.getProductCode(), request.getProductName(),
                    request.getPoints(), request.getStatus(), request.getRejectReason(),
                    request.getCreatedAt(), request.getHandledAt());
        }
    }

    /** 지갑 화면 한 번에 필요한 값. products는 교환할 수 있는 상품 목록(포인트 오름차순). */
    public record WalletResponse(int balance, int pendingExchangePoints, int monthEarned,
                                 List<TransactionResponse> transactions, List<ExchangeResponse> exchanges,
                                 List<RewardProduct> products) {

        public static WalletResponse of(RewardSummary summary, List<RewardProduct> products) {
            return new WalletResponse(summary.balance(), summary.pendingExchangePoints(), summary.monthEarned(),
                    summary.transactions().stream().map(TransactionResponse::from).toList(),
                    summary.exchanges().stream().map(ExchangeResponse::from).toList(),
                    products);
        }
    }

    /**
     * 관리자 교환 승인 큐. approvedSubmissions/rejectedSubmissions는 신청자의 미션 통과·반려 수로,
     * 부정 참여 의심을 판단하는 참고 신호다.
     */
    public record AdminExchangeResponse(Long id, Long userId, String nickname, String productCode,
                                        String productName, int points, ExchangeStatus status,
                                        String rejectReason, LocalDateTime createdAt, LocalDateTime handledAt,
                                        String handledByNickname, long approvedSubmissions,
                                        long rejectedSubmissions) {

        public static AdminExchangeResponse of(RewardExchangeRequest request, long approvedSubmissions,
                                               long rejectedSubmissions) {
            return new AdminExchangeResponse(request.getId(), request.getUser().getId(),
                    request.getUser().getNickname(), request.getProductCode(), request.getProductName(),
                    request.getPoints(), request.getStatus(), request.getRejectReason(), request.getCreatedAt(),
                    request.getHandledAt(),
                    request.getHandledBy() == null ? null : request.getHandledBy().getNickname(),
                    approvedSubmissions, rejectedSubmissions);
        }
    }
}

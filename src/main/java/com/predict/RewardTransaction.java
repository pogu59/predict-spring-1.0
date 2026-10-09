package com.predict;

import com.predict.enums.RewardTransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 리워드 포인트 원장. 추가만 하고 수정·삭제하지 않는다. idempotency_key(UNIQUE)로 같은 사건이
 * 두 번 기록되는 것을 막는다 — 예: "earn:submission:42", "bonus:daily:7:2026-10-09",
 * "exchange:15", "refund:15".
 */
@Getter
@Entity
@Table(name = "reward_transactions")
public class RewardTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "type", nullable = false, length = 20)
    private RewardTransactionType type;

    /** 부호 있는 변동량. 적립은 +, 교환 신청은 -. */
    @Column(name = "amount", nullable = false)
    private int amount;

    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id")
    private MissionSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_id")
    private RewardExchangeRequest exchange;

    /** 내역 화면에 보여 줄 한 줄(미션 제목, "교환 신청 · 상품명" 등). */
    @Column(name = "memo", length = 200)
    private String memo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected RewardTransaction() {
    }

    public RewardTransaction(User user, RewardTransactionType type, int amount, int balanceAfter,
                             String idempotencyKey, MissionSubmission submission,
                             RewardExchangeRequest exchange, String memo) {
        this.user = user;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.idempotencyKey = idempotencyKey;
        this.submission = submission;
        this.exchange = exchange;
        this.memo = memo;
    }
}

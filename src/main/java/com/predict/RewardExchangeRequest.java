package com.predict;

import com.predict.enums.ExchangeStatus;
import com.predict.reward.RewardProduct;
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
 * 기프티콘 교환 신청. 신청하는 순간 포인트를 차감해 두고(에스크로), 운영자가 기프티콘을 보낸 뒤
 * 발송 완료(sent)로 바꾼다. 반려·취소되면 RewardService가 같은 포인트를 환불 기록과 함께 돌려준다.
 * 상태는 requested에서만 바뀔 수 있다.
 */
@Getter
@Entity
@Table(name = "reward_exchange_requests")
public class RewardExchangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exchange_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    /** 신청 시점의 상품명·포인트를 복사해 둔다(카탈로그가 바뀌어도 기록은 그대로). */
    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "points", nullable = false)
    private int points;

    @Column(name = "status", nullable = false, length = 20)
    private ExchangeStatus status = ExchangeStatus.REQUESTED;

    @Column(name = "reject_reason", length = 200)
    private String rejectReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by")
    private User handledBy;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected RewardExchangeRequest() {
    }

    public RewardExchangeRequest(User user, RewardProduct product) {
        this.user = user;
        this.productCode = product.code();
        this.productName = product.name();
        this.points = product.points();
    }

    public void markSent(User admin) {
        requireRequested();
        this.status = ExchangeStatus.SENT;
        this.handledBy = admin;
        this.handledAt = LocalDateTime.now();
    }

    public void reject(User admin, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("반려 사유를 골라 주세요");
        }
        requireRequested();
        this.status = ExchangeStatus.REJECTED;
        this.rejectReason = reason.strip();
        this.handledBy = admin;
        this.handledAt = LocalDateTime.now();
    }

    /** 신청자 본인만, 운영자가 확인하기 전(requested)에만 취소할 수 있다. */
    public void cancelBy(User requester) {
        if (!isOwnedBy(requester)) {
            throw new IllegalArgumentException("내 교환 신청만 취소할 수 있어요");
        }
        requireRequested();
        this.status = ExchangeStatus.CANCELED;
        this.handledAt = LocalDateTime.now();
    }

    public boolean isOwnedBy(User someone) {
        return someone != null && user.getId() != null && user.getId().equals(someone.getId());
    }

    private void requireRequested() {
        if (status != ExchangeStatus.REQUESTED) {
            throw new IllegalStateException("이미 처리된 교환 신청이에요");
        }
    }
}

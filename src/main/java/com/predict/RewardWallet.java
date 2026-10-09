package com.predict;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 리워드 포인트 잔액. 신용도(users.credibility_score)와 섞이지 않도록 users가 아닌 별도 테이블에 둔다.
 * 잔액은 earn()/spend()로만 바뀌고 0 아래로 내려가지 않는다. 같은 지갑을 동시에 바꾸면 @Version
 * 낙관적 락으로 한쪽이 실패한다(중복 적립·마이너스 잔액 방지).
 */
@Getter
@Entity
@Table(name = "reward_wallets")
public class RewardWallet {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "balance", nullable = false)
    private int balance;

    /** 새 지갑은 null — Spring Data가 persist(신규)로 판단하게 래퍼 타입을 쓴다. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected RewardWallet() {
    }

    public RewardWallet(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("지갑 주인이 필요해요");
        }
        this.userId = userId;
        this.balance = 0;
    }

    /** @return 적립 후 잔액(reward_transactions.balance_after에 그대로 기록) */
    public int earn(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("적립 포인트는 1 이상이어야 해요");
        }
        this.balance += amount;
        return this.balance;
    }

    /** @return 차감 후 잔액 */
    public int spend(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("사용 포인트는 1 이상이어야 해요");
        }
        if (amount > balance) {
            throw new IllegalStateException("포인트가 부족해요");
        }
        this.balance -= amount;
        return this.balance;
    }
}

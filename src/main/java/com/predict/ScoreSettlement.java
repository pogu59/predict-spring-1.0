package com.predict;

import com.predict.enums.Choice;
import com.predict.enums.SettlementResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 점수 정산 결과. votes와 1:1 관계이며 투표 1건당 정산 1건만 존재한다.
 */
@Getter
@Entity
@Table(name = "score_settlements")
public class ScoreSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vote_id", nullable = false, unique = true)
    private Vote vote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    /** votes에서 복사해 둔 값(조회 편의용) */
    @Column(name = "choice", nullable = false, length = 10)
    private Choice choice;

    @Column(name = "result", nullable = false, length = 10)
    private SettlementResult result;

    /** 득표비율 p. 반올림 오차 없는 DECIMAL(5,4) 그대로 매핑 */
    @Column(name = "p_value", nullable = false, precision = 5, scale = 4)
    private BigDecimal pValue;

    @Column(name = "score_delta", nullable = false)
    private int scoreDelta;

    /** 정산 직후 누적 신용도 점수 스냅샷 */
    @Column(name = "score_after", nullable = false)
    private int scoreAfter;

    @CreationTimestamp
    @Column(name = "settled_at", nullable = false, updatable = false)
    private LocalDateTime settledAt;

    protected ScoreSettlement() {
    }

    public ScoreSettlement(Vote vote, User user, Topic topic, Choice choice,
                            SettlementResult result, BigDecimal pValue,
                            int scoreDelta, int scoreAfter) {
        this.vote = vote;
        this.user = user;
        this.topic = topic;
        this.choice = choice;
        this.result = result;
        this.pValue = pValue;
        this.scoreDelta = scoreDelta;
        this.scoreAfter = scoreAfter;
    }

}

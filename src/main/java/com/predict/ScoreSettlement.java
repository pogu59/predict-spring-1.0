package com.predict;

import com.predict.enums.SettlementResult;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 점수 정산 결과. 평소엔 투표 1건당 정산 1건(1:1)이지만, 오확정 정정(SettlementCorrectionService)
 * 후 재확정되면 같은 투표에 새 정산 기록이 또 생길 수 있어 vote_id는 UNIQUE가 아니다
 * (schema_8.sql score_settlements.vote_id 주석 참고). 특정 시점에 유효한 정산은
 * is_reversed=false인 것 하나뿐이라는 게 불변식이다.
 */
@Getter
@Entity
@Table(name = "score_settlements")
public class ScoreSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vote_id", nullable = false)
    private Vote vote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    /** votes에서 복사해 둔 값(조회 편의용) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_option_id", nullable = false)
    private IssueOption issueOption;

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

    /**
     * 오확정 정정으로 무효화된 기록인지(1=무효). 삭제하지 않고 감사기록으로 보존하며,
     * 점수 재생(replay) 시 이 값이 true인 기록은 건너뛴다.
     */
    @Column(name = "is_reversed", nullable = false, columnDefinition = "boolean default false")
    private boolean isReversed = false;

    protected ScoreSettlement() {
    }

    public ScoreSettlement(Vote vote, User user, Issue issue, IssueOption issueOption,
                            SettlementResult result, BigDecimal pValue,
                            int scoreDelta, int scoreAfter) {
        this.vote = vote;
        this.user = user;
        this.issue = issue;
        this.issueOption = issueOption;
        this.result = result;
        this.pValue = pValue;
        this.scoreDelta = scoreDelta;
        this.scoreAfter = scoreAfter;
    }

    /** 오확정 정정 시 호출 — 삭제 대신 무효 표시만 한다. */
    public void reverse() {
        this.isReversed = true;
    }

}

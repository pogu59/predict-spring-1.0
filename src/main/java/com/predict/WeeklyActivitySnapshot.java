package com.predict;

import com.predict.enums.Tier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 다이아/마스터 주간 활동성 체크 스냅샷. 매주 일요일 자정 배치가 계산한 결과를 그대로 영구 저장한다.
 * 유저당 주 하나에 기록 하나만 존재한다(uq_snapshot_user_week).
 */
@Getter
@Entity
@Table(name = "weekly_activity_snapshots",
        uniqueConstraints = @UniqueConstraint(name = "uq_snapshot_user_week", columnNames = {"user_id", "week_start"}))
public class WeeklyActivitySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 해당 주 월요일 날짜 */
    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    /** 해당 주 일요일 날짜 */
    @Column(name = "week_end", nullable = false)
    private LocalDate weekEnd;

    /** 그 주 실제 투표 횟수(계산 당시 값 고정) */
    @Column(name = "vote_count", nullable = false)
    private int voteCount;

    /** 5회 조건 충족 여부 */
    @Column(name = "met_requirement", nullable = false)
    private boolean metRequirement;

    @Column(name = "tier_before", nullable = false, length = 20)
    private Tier tierBefore;

    @Column(name = "tier_after", nullable = false, length = 20)
    private Tier tierAfter;

    @CreationTimestamp
    @Column(name = "checked_at", nullable = false, updatable = false)
    private LocalDateTime checkedAt;

    protected WeeklyActivitySnapshot() {
    }

    public WeeklyActivitySnapshot(User user, LocalDate weekStart, LocalDate weekEnd,
                                   int voteCount, boolean metRequirement,
                                   Tier tierBefore, Tier tierAfter) {
        this.user = user;
        this.weekStart = weekStart;
        this.weekEnd = weekEnd;
        this.voteCount = voteCount;
        this.metRequirement = metRequirement;
        this.tierBefore = tierBefore;
        this.tierAfter = tierAfter;
    }

}

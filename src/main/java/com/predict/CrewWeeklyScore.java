package com.predict;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 크루 대항전 주간 스냅샷. 매주 일요일 자정(월요일 00:00 KST) 배치가 방금 끝난 주를 계산해 고정한다.
 * rank가 null이면 활성 멤버가 모자라 순위에서 빠진("집계 중") 크루다.
 */
@Getter
@Entity
@Table(name = "crew_weekly_scores",
        uniqueConstraints = @UniqueConstraint(name = "uq_crew_week", columnNames = {"crew_id", "week_start"}))
public class CrewWeeklyScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crew_weekly_score_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    /** 그 주 월요일 */
    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "active_members", nullable = false)
    private int activeMembers;

    @Column(name = "score_sum", nullable = false)
    private int scoreSum;

    @Column(name = "score_per_member", nullable = false, precision = 8, scale = 2)
    private BigDecimal scorePerMember;

    @Column(name = "`rank`")
    private Integer rank;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected CrewWeeklyScore() {
    }

    public CrewWeeklyScore(Crew crew, LocalDate weekStart, int activeMembers, int scoreSum,
                            BigDecimal scorePerMember, Integer rank) {
        this.crew = crew;
        this.weekStart = weekStart;
        this.activeMembers = activeMembers;
        this.scoreSum = scoreSum;
        this.scorePerMember = scorePerMember;
        this.rank = rank;
    }
}

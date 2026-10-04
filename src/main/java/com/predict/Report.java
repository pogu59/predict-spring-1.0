package com.predict;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.LocalDateTime;

/**
 * 게시글/댓글 신고. 같은 유저가 같은 대상을 여러 번 신고해도 1건만 남는다(uq_reports_reporter).
 * 관리자 화면은 대상별로 묶어 "누적 n건"으로 보여준다(AdminCommunityService).
 */
@Getter
@Entity
@Table(name = "reports",
        uniqueConstraints = @UniqueConstraint(name = "uq_reports_reporter",
                columnNames = {"target_type", "target_id", "reporter_id"}))
public class Report {

    public enum TargetType { POST, REPLY }

    public enum Status { PENDING, REJECTED, REMOVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 10)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    /** 프론트 사유 시트의 라벨 그대로(스팸·광고 / 욕설·비하 / 음란·선정성 / 개인정보 노출 / 기타). */
    @Column(name = "reason", nullable = false, length = 30)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private Status status = Status.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    protected Report() {
    }

    public Report(TargetType targetType, Long targetId, User reporter, String reason) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.reporter = reporter;
        this.reason = reason;
    }

    public void process(Status status, LocalDateTime processedAt) {
        this.status = status;
        this.processedAt = processedAt;
    }
}

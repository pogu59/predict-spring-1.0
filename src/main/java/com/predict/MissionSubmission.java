package com.predict;

import com.predict.enums.SubmissionStatus;
import com.predict.mission.QualityResult;
import com.predict.support.IntListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import java.util.List;

/**
 * 미션 제출. 같은 미션·같은 유저·같은 날짜에는 한 건만 들어간다(uq_submissions_mission_user_day) —
 * 출석은 이걸로 하루 1회가 되고, 출석 외 미션의 "미션당 1회"는 MissionService에서 추가로 막는다.
 */
@Getter
@Entity
@Table(name = "mission_submissions",
        uniqueConstraints = @UniqueConstraint(name = "uq_submissions_mission_user_day",
                columnNames = {"mission_id", "user_id", "submitted_on"}))
public class MissionSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submission_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "submitted_on", nullable = false)
    private LocalDate submittedOn;

    /** 문항 순서대로 고른 보기 인덱스. 출석은 빈 목록. */
    @Convert(converter = IntListConverter.class)
    @Column(name = "answers", nullable = false, length = 500)
    private List<Integer> answers;

    /** 미션 화면을 연 뒤 제출까지 걸린 시간(클라이언트 측정). 너무 빠른 응답 판정에 쓴다. */
    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "quality_score", nullable = false)
    private int qualityScore;

    @Column(name = "status", nullable = false, length = 20)
    private SubmissionStatus status;

    /** 반려 이유. 참여자 화면에 그대로 보여 준다(해요체). */
    @Column(name = "reject_reason", length = 200)
    private String rejectReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected MissionSubmission() {
    }

    public MissionSubmission(Mission mission, User user, LocalDate submittedOn, List<Integer> answers,
                             long durationMs, QualityResult quality) {
        this.mission = mission;
        this.user = user;
        this.submittedOn = submittedOn;
        this.answers = List.copyOf(answers);
        this.durationMs = durationMs;
        this.qualityScore = quality.score();
        this.status = quality.status();
        this.rejectReason = quality.rejectReason();
    }

    public boolean isApproved() {
        return status == SubmissionStatus.APPROVED;
    }
}

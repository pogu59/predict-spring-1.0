package com.predict;

import com.predict.enums.MissionStatus;
import com.predict.enums.MissionType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 참여자가 하는 미션(출석·밸런스 게임·설문). 관리자가 draft로 만들고 문항을 채운 뒤 open한다.
 * is_daily=true인 미션은 "오늘의 미션"으로 묶이고, 그날 열린 오늘의 미션(출석 제외)을 모두
 * 끝내면 보너스를 준다(MissionService.submit).
 */
@Getter
@Entity
@Table(name = "missions")
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mission_id")
    private Long id;

    @Column(name = "type", nullable = false, length = 20)
    private MissionType type;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    /** 검수를 통과하면 주는 리워드 포인트. 신용도(users.credibility_score)와는 완전히 별개다. */
    @Column(name = "reward_points", nullable = false)
    private int rewardPoints;

    @Column(name = "is_daily", nullable = false)
    private boolean daily;

    @Column(name = "status", nullable = false, length = 20)
    private MissionStatus status = MissionStatus.DRAFT;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @OneToMany(mappedBy = "mission", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<MissionQuestion> questions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Mission() {
    }

    public Mission(MissionType type, String title, String description, int rewardPoints, boolean daily,
                   LocalDateTime startsAt, LocalDateTime endsAt) {
        if (type == null) {
            throw new IllegalArgumentException("미션 종류를 골라 주세요");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("미션 제목을 입력해 주세요");
        }
        if (rewardPoints < 0) {
            throw new IllegalArgumentException("포인트는 0 이상이어야 해요");
        }
        if (startsAt == null || endsAt == null || !startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("시작 시각이 종료 시각보다 빨라야 해요");
        }
        this.type = type;
        this.title = title.strip();
        this.description = description == null || description.isBlank() ? null : description.strip();
        this.rewardPoints = rewardPoints;
        this.daily = daily;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    /**
     * 문항을 순서대로 붙인다. attentionAnswerIndex가 있으면 확인(주의) 문항이고, 그 보기를 고르지
     * 않은 제출은 반려된다. 공개된 뒤에는 문항을 바꿀 수 없다(이미 응답한 사람과 질문이 달라진다).
     */
    public MissionQuestion addQuestion(String text, List<String> options, Integer attentionAnswerIndex) {
        if (type == MissionType.ATTENDANCE) {
            throw new IllegalStateException("출석 미션에는 문항을 넣을 수 없어요");
        }
        if (status != MissionStatus.DRAFT) {
            throw new IllegalStateException("공개한 미션의 문항은 바꿀 수 없어요");
        }
        if (type == MissionType.BALANCE && !questions.isEmpty()) {
            throw new IllegalStateException("밸런스 게임은 문항이 하나예요");
        }
        MissionQuestion question = new MissionQuestion(this, questions.size(), text, options, attentionAnswerIndex);
        questions.add(question);
        return question;
    }

    public void open() {
        if (status == MissionStatus.OPEN) {
            return;
        }
        if (type != MissionType.ATTENDANCE && questions.isEmpty()) {
            throw new IllegalStateException("문항이 없는 미션은 열 수 없어요");
        }
        this.status = MissionStatus.OPEN;
    }

    public void close() {
        this.status = MissionStatus.CLOSED;
    }

    /** 공개 상태이고 [startsAt, endsAt) 안일 때만 참여할 수 있다. */
    public boolean isAvailableAt(LocalDateTime now) {
        return status == MissionStatus.OPEN && !now.isBefore(startsAt) && now.isBefore(endsAt);
    }

    public List<MissionQuestion> getQuestions() {
        return Collections.unmodifiableList(questions);
    }
}

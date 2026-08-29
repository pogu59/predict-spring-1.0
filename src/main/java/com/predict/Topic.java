package com.predict;

import com.predict.enums.Choice;
import com.predict.enums.TopicStatus;
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
 * 투표 주제. yes_count/no_count는 마감 시점 득표수를 정산용으로 고정해 둔 스냅샷이다.
 */
@Getter
@Entity
@Table(name = "topics")
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "topic_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "status", nullable = false, length = 20)
    private TopicStatus status;

    @Column(name = "vote_start_at", nullable = false)
    private LocalDateTime voteStartAt;

    @Column(name = "vote_deadline_at", nullable = false)
    private LocalDateTime voteDeadlineAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    /** 정답. void 처리된 주제는 끝까지 null로 유지된다. */
    @Column(name = "correct_answer")
    private Choice correctAnswer;

    @Column(name = "yes_count", nullable = false)
    private int yesCount;

    @Column(name = "no_count", nullable = false)
    private int noCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Topic() {
    }

    public Topic(Category category, String title, String description,
                 LocalDateTime voteStartAt, LocalDateTime voteDeadlineAt) {
        this.category = category;
        this.title = title;
        this.description = description;
        this.voteStartAt = voteStartAt;
        this.voteDeadlineAt = voteDeadlineAt;
        this.status = TopicStatus.OPEN;
        this.yesCount = 0;
        this.noCount = 0;
    }

    /** 마감 시점 득표수를 스냅샷으로 기록하고 결과대기 상태로 전환한다. */
    public void closeForResult(int yesCount, int noCount) {
        this.yesCount = yesCount;
        this.noCount = noCount;
        this.status = TopicStatus.PENDING_RESULT;
    }

    /** 정답을 확정한다. */
    public void confirm(Choice correctAnswer, LocalDateTime confirmedAt) {
        this.correctAnswer = correctAnswer;
        this.confirmedAt = confirmedAt;
        this.status = TopicStatus.CONFIRMED;
    }

    /** 무효 처리한다. correct_answer는 채우지 않는다. */
    public void voidTopic(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
        this.status = TopicStatus.VOID;
    }

}

package com.predict;

import com.predict.enums.TopicStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 투표 주제. 선택지(TopicOption)별 득표수는 마감 시점에 스냅샷으로 고정해 둔다(정산용).
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

    /** 누가 이 결과를 확정했는지(책임 소재 추적용). 정정 시 다시 null로 되돌아간다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;

    /** 정답 선택지. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "correct_option_id")
    private TopicOption correctOption;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<TopicOption> options = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Topic() {
    }

    public Topic(Category category, String title, String description,
                 LocalDateTime voteStartAt, LocalDateTime voteDeadlineAt, List<String> optionTexts) {
        this.category = category;
        this.title = title;
        this.description = description;
        this.voteStartAt = voteStartAt;
        this.voteDeadlineAt = voteDeadlineAt;
        this.status = TopicStatus.OPEN;
        addOptions(optionTexts);
    }

    /** 마감 시점 득표수를 선택지별 스냅샷으로 기록하고 결과대기 상태로 전환한다. */
    public void closeForResult(Map<Long, Integer> voteCountsByOptionId) {
        for (TopicOption option : options) {
            option.recordVoteCount(voteCountsByOptionId.getOrDefault(option.getId(), 0));
        }
        this.status = TopicStatus.PENDING_RESULT;
    }

    /** 정답을 확정한다. */
    public void confirm(TopicOption correctOption, LocalDateTime confirmedAt, User confirmedBy) {
        this.correctOption = correctOption;
        this.confirmedAt = confirmedAt;
        this.confirmedBy = confirmedBy;
        this.status = TopicStatus.CONFIRMED;
    }

    /**
     * 오확정 정정(SettlementCorrectionService)에서 호출. 결과대기 상태로 되돌려
     * sp_confirm_topic_result에 해당하는 정산 절차를 올바른 정답으로 재실행할 수 있게 한다.
     */
    public void resetForCorrection() {
        this.correctOption = null;
        this.confirmedAt = null;
        this.confirmedBy = null;
        this.status = TopicStatus.PENDING_RESULT;
    }

    /** 참여자 0명일 때만 허용되는 전체 내용 수정(AdminTopicService). 선택지도 통째로 교체된다. */
    public void updateContent(Category category, String title, String description,
                               LocalDateTime voteStartAt, LocalDateTime voteDeadlineAt, List<String> optionTexts) {
        this.category = category;
        this.title = title;
        this.description = description;
        this.voteStartAt = voteStartAt;
        this.voteDeadlineAt = voteDeadlineAt;
        this.options.clear();
        addOptions(optionTexts);
    }

    /** 마감시각 연장 전용(AdminTopicService). 단축은 허용하지 않는다(서비스 레이어에서 검증). */
    public void extendDeadline(LocalDateTime newDeadline) {
        this.voteDeadlineAt = newDeadline;
    }

    private void addOptions(List<String> optionTexts) {
        int order = 0;
        for (String text : optionTexts) {
            this.options.add(new TopicOption(this, text, order++));
        }
    }

}

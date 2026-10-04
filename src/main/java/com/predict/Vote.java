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

import java.time.LocalDateTime;

/**
 * 투표 참여 기록. 유저당 주제 1표만 허용된다(uq_votes_user_issue). 마감 전까지는 선택지만
 * 바꿀 수 있고(changeOption), 베팅액(stake)은 최초 투표 때 건 그대로 유지된다.
 */
@Getter
@Entity
@Table(name = "votes",
        uniqueConstraints = @UniqueConstraint(name = "uq_votes_user_issue", columnNames = {"user_id", "issue_id"}))
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vote_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_option_id", nullable = false)
    private IssueOption issueOption;

    /**
     * 이 투표에 건 신용도. 투표 시점에 유저 잔액에서 즉시 차감(에스크로)되고,
     * 정산 시점에 stake + scoreDelta(정답)/stake + scoreDelta(오답, delta가 음수)만큼
     * 돌려받는다 — VoteService.castVote / SettlementService.confirmIssue 참고.
     */
    @Column(name = "stake", nullable = false)
    private int stake;

    @CreationTimestamp
    @Column(name = "voted_at", nullable = false, updatable = false)
    private LocalDateTime votedAt;

    protected Vote() {
    }

    public Vote(User user, Issue issue, IssueOption issueOption, int stake) {
        this.user = user;
        this.issue = issue;
        this.issueOption = issueOption;
        this.stake = stake;
    }

    public void changeOption(IssueOption issueOption) {
        this.issueOption = issueOption;
    }

}

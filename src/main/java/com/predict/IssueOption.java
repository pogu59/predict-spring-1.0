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
import lombok.Getter;

/**
 * 주제(Issue)의 선택지. 관리자가 등록 시 텍스트로 직접 입력한다(최소 2개, 최대 6개).
 */
@Getter
@Entity
@Table(name = "issue_options")
public class IssueOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issue_option_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @Column(name = "text", nullable = false, length = 255)
    private String text;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    /** 마감 시점 득표수 스냅샷(정산용). OPEN 상태인 동안은 null. */
    @Column(name = "vote_count")
    private Integer voteCount;

    protected IssueOption() {
    }

    public IssueOption(Issue issue, String text, int displayOrder) {
        this.issue = issue;
        this.text = text;
        this.displayOrder = displayOrder;
    }

    public void recordVoteCount(int voteCount) {
        this.voteCount = voteCount;
    }

    public void clearVoteCount() {
        this.voteCount = null;
    }
}

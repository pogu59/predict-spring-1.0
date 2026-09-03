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
 * 주제(Topic)의 선택지. 관리자가 등록 시 텍스트로 직접 입력하며 개수 제한은 없다(최소 2개).
 */
@Getter
@Entity
@Table(name = "topic_options")
public class TopicOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "topic_option_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "text", nullable = false, length = 255)
    private String text;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    /** 마감 시점 득표수 스냅샷(정산용). OPEN 상태인 동안은 null. */
    @Column(name = "vote_count")
    private Integer voteCount;

    protected TopicOption() {
    }

    public TopicOption(Topic topic, String text, int displayOrder) {
        this.topic = topic;
        this.text = text;
        this.displayOrder = displayOrder;
    }

    public void recordVoteCount(int voteCount) {
        this.voteCount = voteCount;
    }
}

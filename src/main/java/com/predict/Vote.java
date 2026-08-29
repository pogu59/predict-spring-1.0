package com.predict;

import com.predict.enums.Choice;
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
 * 투표 참여 기록. 유저당 주제 1표만 허용된다(uq_votes_user_topic).
 */
@Getter
@Entity
@Table(name = "votes",
        uniqueConstraints = @UniqueConstraint(name = "uq_votes_user_topic", columnNames = {"user_id", "topic_id"}))
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vote_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "choice", nullable = false, length = 10)
    private Choice choice;

    @CreationTimestamp
    @Column(name = "voted_at", nullable = false, updatable = false)
    private LocalDateTime votedAt;

    protected Vote() {
    }

    public Vote(User user, Topic topic, Choice choice) {
        this.user = user;
        this.topic = topic;
        this.choice = choice;
    }

}

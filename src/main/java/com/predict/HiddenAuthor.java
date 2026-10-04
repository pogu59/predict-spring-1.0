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

/** "이 사용자의 글 숨기기" — user가 hiddenUser의 글을 게시판 목록에서 보지 않도록 기록한다. */
@Getter
@Entity
@Table(name = "hidden_authors",
        uniqueConstraints = @UniqueConstraint(name = "uq_hidden_authors", columnNames = {"user_id", "hidden_user_id"}))
public class HiddenAuthor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hidden_author_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hidden_user_id", nullable = false)
    private User hiddenUser;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected HiddenAuthor() {
    }

    public HiddenAuthor(User user, User hiddenUser) {
        this.user = user;
        this.hiddenUser = hiddenUser;
    }
}

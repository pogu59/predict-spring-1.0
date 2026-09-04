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
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 댓글. 이슈 상세 댓글과 게시판 댓글을 같은 테이블(replies)에서 함께 다룬다 — issue/post 중
 * 정확히 하나만 채워지며(DB의 chk_replies_one_target CHECK로도 강제), 어느 쪽에 달린 댓글인지는
 * null이 아닌 쪽으로 판단한다. 생성은 반드시 forIssue/forPost 팩토리로만 하여 둘 다 채우거나
 * 둘 다 비우는 실수를 막는다. 대댓글은 없다(플랫 목록) — 필요해지면 parent_reply_id 컬럼 추가.
 */
@Getter
@Entity
@Table(name = "replies")
public class Reply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reply_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id")
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "boolean default false")
    private boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected Reply() {
    }

    private Reply(User author, Issue issue, Post post, String content) {
        this.author = author;
        this.issue = issue;
        this.post = post;
        this.content = content;
    }

    public static Reply forIssue(User author, Issue issue, String content) {
        return new Reply(author, issue, null, content);
    }

    public static Reply forPost(User author, Post post, String content) {
        return new Reply(author, null, post, content);
    }

    public void delete(LocalDateTime deletedAt) {
        this.deleted = true;
        this.deletedAt = deletedAt;
    }

    public boolean isAuthor(User user) {
        return this.author.getId().equals(user.getId());
    }
}

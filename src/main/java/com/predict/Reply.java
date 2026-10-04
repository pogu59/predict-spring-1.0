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
 * 둘 다 비우는 실수를 막는다. 게시판 댓글은 1단계 대댓글(parent)을 가질 수 있다 — 대댓글의
 * 대댓글은 만들지 않고 최상위 댓글에 붙인다(ReplyService.createForPost).
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

    /** 게시판 대댓글의 부모(최상위 댓글). 최상위 댓글이면 null. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_reply_id")
    private Reply parent;

    /** 관리자가 숨긴 댓글. 사용자 목록에서는 빠지고 관리자 화면에서만 보인다. */
    @Column(name = "is_hidden", nullable = false, columnDefinition = "boolean default false")
    private boolean hidden = false;

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
        return forPost(author, post, content, null);
    }

    public static Reply forPost(User author, Post post, String content, Reply parent) {
        Reply reply = new Reply(author, null, post, content);
        reply.parent = parent;
        return reply;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public void delete(LocalDateTime deletedAt) {
        this.deleted = true;
        this.deletedAt = deletedAt;
    }

    public boolean isAuthor(User user) {
        return this.author.getId().equals(user.getId());
    }
}

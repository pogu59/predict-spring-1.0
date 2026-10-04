package com.predict;

import com.predict.enums.PostTopic;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 자유 게시판 글. 작성자는 제목·본문·이미지를 수정할 수 있다. 삭제는 소프트 삭제(deleted/deletedAt)만
 * 하고 content는 남겨둔다(신고 대응·복구용, 목록/상세 조회에서만 제외). 관리자는 글을 숨길 수 있다(hidden).
 */
@Getter
@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 첨부 이미지 URL(UploadController), 최대 4장. */
    @ElementCollection
    @CollectionTable(name = "post_images", joinColumns = @JoinColumn(name = "post_id"))
    @OrderColumn(name = "image_order")
    @Column(name = "url", nullable = false, length = 500)
    private List<String> images = new ArrayList<>();

    /** 말머리(정보·분석·질문·잡담). 말머리가 생기기 전에 쓴 글은 null. */
    @Column(name = "topic", length = 10)
    private PostTopic topic;

    @Column(name = "is_hidden", nullable = false, columnDefinition = "boolean default false")
    private boolean hidden = false;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "boolean default false")
    private boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected Post() {
    }

    public Post(User author, String title, String content) {
        this(author, title, content, List.of(), null);
    }

    public Post(User author, String title, String content, List<String> images, PostTopic topic) {
        this.author = author;
        this.title = title;
        this.content = content;
        this.images.addAll(images);
        this.topic = topic;
    }

    public void update(String title, String content, List<String> images, PostTopic topic) {
        this.title = title;
        this.content = content;
        this.images.clear();
        this.images.addAll(images);
        this.topic = topic;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void delete(LocalDateTime deletedAt) {
        this.deleted = true;
        this.deletedAt = deletedAt;
    }

    public boolean isAuthor(User user) {
        return this.author.getId().equals(user.getId());
    }
}

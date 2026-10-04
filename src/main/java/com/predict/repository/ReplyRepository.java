package com.predict.repository;

import com.predict.Reply;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReplyRepository extends JpaRepository<Reply, Long> {

    /** 사용자 화면 — 삭제·관리자 숨김 제외. */
    List<Reply> findByIssueIdAndDeletedFalseAndHiddenFalseOrderByCreatedAtAsc(Long issueId);

    List<Reply> findByPostIdAndDeletedFalseAndHiddenFalseOrderByCreatedAtAsc(Long postId);

    /** 게시판 목록에서 글별 댓글 수 표시용. */
    long countByPostIdAndDeletedFalseAndHiddenFalse(Long postId);

    /** 게시판 목록의 글별 댓글 수 — [postId, count] 행. */
    @Query("""
            SELECT r.post.id, COUNT(r) FROM Reply r
            WHERE r.post.id IN :postIds AND r.deleted = false AND r.hidden = false
            GROUP BY r.post.id
            """)
    List<Object[]> countByPostIds(@Param("postIds") Collection<Long> postIds);

    /** 관리자 커뮤니티 관리 — 숨김 포함, 삭제 제외, 최신순. */
    List<Reply> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);
}

package com.predict.repository;

import com.predict.Reply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReplyRepository extends JpaRepository<Reply, Long> {

    List<Reply> findByIssueIdAndDeletedFalseOrderByCreatedAtAsc(Long issueId);

    List<Reply> findByPostIdAndDeletedFalseOrderByCreatedAtAsc(Long postId);

    /** 게시판 목록에서 글별 댓글 수 표시용. */
    long countByPostIdAndDeletedFalse(Long postId);
}

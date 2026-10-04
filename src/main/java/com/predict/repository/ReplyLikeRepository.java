package com.predict.repository;

import com.predict.ReplyLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReplyLikeRepository extends JpaRepository<ReplyLike, Long> {

    Optional<ReplyLike> findByReplyIdAndUserId(Long replyId, Long userId);

    long countByReplyId(Long replyId);

    /** 댓글별 좋아요 수 — [replyId, count] 행. */
    @Query("SELECT l.reply.id, COUNT(l) FROM ReplyLike l WHERE l.reply.id IN :replyIds GROUP BY l.reply.id")
    List<Object[]> countByReplyIds(@Param("replyIds") Collection<Long> replyIds);

    @Query("SELECT l.reply.id FROM ReplyLike l WHERE l.user.id = :userId AND l.reply.id IN :replyIds")
    List<Long> findLikedReplyIds(@Param("userId") Long userId, @Param("replyIds") Collection<Long> replyIds);
}

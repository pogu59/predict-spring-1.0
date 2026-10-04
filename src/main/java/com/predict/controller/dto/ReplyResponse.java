package com.predict.controller.dto;

import com.predict.Reply;

import java.time.LocalDateTime;
import java.util.List;

/**
 * authorId를 그대로 내려주므로 "내 댓글인지"는 프론트가 /api/auth/me의 userId와 비교해서 판단한다.
 * likedByMe는 Authorization 헤더가 있을 때만 채워진다(없으면 false).
 * authorOptionId: 이슈 댓글 전용 — 작성자가 이 이슈에 투표했다면 현재 선택지 id.
 * replies: 게시판 댓글 전용 — 1단계 대댓글.
 */
public record ReplyResponse(
        Long id,
        Long authorId,
        String authorNickname,
        String content,
        LocalDateTime createdAt,
        long likeCount,
        boolean likedByMe,
        Long authorOptionId,
        Long parentId,
        List<ReplyResponse> replies
) {
    public static ReplyResponse from(Reply reply) {
        return from(reply, 0, false, null, List.of());
    }

    public static ReplyResponse from(Reply reply, long likeCount, boolean likedByMe, Long authorOptionId,
                                     List<ReplyResponse> replies) {
        return new ReplyResponse(
                reply.getId(),
                reply.getAuthor().getId(),
                reply.getAuthor().getNickname(),
                reply.getContent(),
                reply.getCreatedAt(),
                likeCount,
                likedByMe,
                authorOptionId,
                reply.getParent() != null ? reply.getParent().getId() : null,
                replies);
    }
}

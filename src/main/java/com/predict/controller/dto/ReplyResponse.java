package com.predict.controller.dto;

import com.predict.Reply;

import java.time.LocalDateTime;

/**
 * authorId를 그대로 내려주므로 "내 댓글인지"는 프론트가 /api/auth/me의 userId와 비교해서
 * 판단한다 — 목록 조회는 인증이 필요 없는 공개 API라 서버가 매번 requester를 알 수 없다.
 */
public record ReplyResponse(
        Long id,
        Long authorId,
        String authorNickname,
        String content,
        LocalDateTime createdAt
) {
    public static ReplyResponse from(Reply reply) {
        return new ReplyResponse(
                reply.getId(),
                reply.getAuthor().getId(),
                reply.getAuthor().getNickname(),
                reply.getContent(),
                reply.getCreatedAt());
    }
}

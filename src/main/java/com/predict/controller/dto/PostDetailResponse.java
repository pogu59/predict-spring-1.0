package com.predict.controller.dto;

import com.predict.Post;

import java.time.LocalDateTime;

/** authorId를 그대로 내려주므로 "내 글인지"는 ReplyResponse와 같은 이유로 프론트가 판단한다. */
public record PostDetailResponse(
        Long id,
        Long authorId,
        String authorNickname,
        String title,
        String content,
        int viewCount,
        LocalDateTime createdAt
) {
    public static PostDetailResponse from(Post post) {
        return new PostDetailResponse(
                post.getId(),
                post.getAuthor().getId(),
                post.getAuthor().getNickname(),
                post.getTitle(),
                post.getContent(),
                post.getViewCount(),
                post.getCreatedAt());
    }
}

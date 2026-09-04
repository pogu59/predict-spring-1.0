package com.predict.controller.dto;

import com.predict.Post;

import java.time.LocalDateTime;

public record PostListItemResponse(
        Long id,
        String authorNickname,
        String title,
        int viewCount,
        long replyCount,
        LocalDateTime createdAt
) {
    public static PostListItemResponse from(Post post, long replyCount) {
        return new PostListItemResponse(
                post.getId(),
                post.getAuthor().getNickname(),
                post.getTitle(),
                post.getViewCount(),
                replyCount,
                post.getCreatedAt());
    }
}

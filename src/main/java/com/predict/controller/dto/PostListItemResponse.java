package com.predict.controller.dto;

import com.predict.Post;

import java.time.LocalDateTime;

/** contentPreview: 본문 첫 줄. thumbnailUrl: 첫 첨부 이미지(없으면 null). */
public record PostListItemResponse(
        Long id,
        String authorNickname,
        String title,
        String contentPreview,
        String thumbnailUrl,
        long likeCount,
        boolean likedByMe,
        int viewCount,
        long replyCount,
        LocalDateTime createdAt
) {
    public static PostListItemResponse from(Post post, long likeCount, boolean likedByMe, long replyCount) {
        String firstLine = post.getContent().strip().split("\\R", 2)[0];
        return new PostListItemResponse(
                post.getId(),
                post.getAuthor().getNickname(),
                post.getTitle(),
                firstLine.length() > 120 ? firstLine.substring(0, 120) : firstLine,
                post.getImages().isEmpty() ? null : post.getImages().get(0),
                likeCount,
                likedByMe,
                post.getViewCount(),
                replyCount,
                post.getCreatedAt());
    }
}

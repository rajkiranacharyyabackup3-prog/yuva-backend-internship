package com.yuvaintern.blog.post;

import java.time.OffsetDateTime;

public record PostResponse(Long id, String title, String content, Long authorId, String authorUsername,
                           OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static PostResponse from(Post post) {
        return new PostResponse(post.getId(), post.getTitle(), post.getContent(),
                post.getAuthor().getId(), post.getAuthor().getUsername(), post.getCreatedAt(), post.getUpdatedAt());
    }
}

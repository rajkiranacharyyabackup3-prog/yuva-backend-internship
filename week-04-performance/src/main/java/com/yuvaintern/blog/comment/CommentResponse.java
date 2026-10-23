package com.yuvaintern.blog.comment;

import java.time.OffsetDateTime;

public record CommentResponse(Long id, Long postId, Long authorId, String authorUsername, String content,
                              OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getPost().getId(), comment.getAuthor().getId(),
                comment.getAuthor().getUsername(), comment.getContent(), comment.getCreatedAt(),
                comment.getUpdatedAt());
    }
}

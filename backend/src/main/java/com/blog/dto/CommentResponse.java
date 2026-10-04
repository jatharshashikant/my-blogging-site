package com.blog.dto;

import com.blog.model.Comment;
import java.time.LocalDateTime;

public record CommentResponse(
    Long id,
    String body,
    String authorName,
    LocalDateTime createdAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
            comment.getId(),
            comment.getBody(),
            comment.getAuthorName(),
            comment.getCreatedAt()
        );
    }
}

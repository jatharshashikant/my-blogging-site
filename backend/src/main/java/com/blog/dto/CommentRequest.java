package com.blog.dto;

public record CommentRequest(
    String body,
    String authorName
) {}

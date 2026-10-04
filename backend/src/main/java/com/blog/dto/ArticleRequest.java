package com.blog.dto;

public record ArticleRequest(
        String title,
        String body,
        String category
) {}

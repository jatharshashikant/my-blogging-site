package com.blog.dto;

public record ErrorResponse(
        String error,
        String message
) {}

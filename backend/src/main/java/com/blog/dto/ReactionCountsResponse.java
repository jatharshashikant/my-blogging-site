package com.blog.dto;

public record ReactionCountsResponse(
    Long love,
    Long like,
    Long dislike,
    String userReaction
) {}

package com.blog.dto;

import com.blog.model.Article;
import java.time.LocalDateTime;

public record ArticleResponse(
        Long id,
        String title,
        String body,
        String author,
        String category,
        int likesCount,
        String documentUrl,
        LocalDateTime publishedAt
) {
    public static ArticleResponse from(Article a) {
        return new ArticleResponse(
                a.getId(),
                a.getTitle(),
                a.getBody(),
                a.getAuthor(),
                a.getCategory(),
                a.getLikesCount(),
                a.getDocumentUrl(),
                a.getPublishedAt()
        );
    }
}

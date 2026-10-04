package com.blog.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "articles")
@Data
@NoArgsConstructor
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body = "";

    @Column(nullable = false)
    private String author = "Admin";

    private String category;

    @Column(nullable = false)
    private int likesCount = 0;

    @Column(length = 20, nullable = false)
    private String source = "ADMIN";  // "ADMIN" | "GUEST"

    @Column(length = 1000)
    private String documentUrl;

    private LocalDateTime publishedAt;

    @PrePersist
    protected void onCreate() {
        if (publishedAt == null) {
            publishedAt = LocalDateTime.now();
        }
    }
}

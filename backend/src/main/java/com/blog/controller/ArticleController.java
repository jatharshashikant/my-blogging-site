package com.blog.controller;

import com.blog.dto.ArticleResponse;
import com.blog.service.ArticleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for public article endpoints.
 * Requirements: 1.1, 1.4, 2.1, 5.2, 5.3, 5.5
 */
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    /**
     * GET /api/articles
     * Returns a paginated list of articles ordered by publishedAt descending.
     * Default page size is 10.
     * Requirements: 1.1, 1.4
     */
    @GetMapping
    public ResponseEntity<Page<ArticleResponse>> listArticles(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(articleService.listArticles(pageable));
    }

    /**
     * GET /api/articles/{id}
     * Returns a single article by ID.
     * Returns 404 via GlobalExceptionHandler if not found.
     * Requirements: 2.1
     */
    @GetMapping("/{id}")
    public ResponseEntity<ArticleResponse> getArticle(@PathVariable Long id) {
        return ResponseEntity.ok(articleService.getArticle(id));
    }

    /**
     * GET /api/articles/search?q=...
     * Searches articles by keyword across title and body (case-insensitive).
     * Returns 400 via GlobalExceptionHandler if q is absent or blank (ValidationException).
     * Requirements: 5.2, 5.3, 5.5
     */
    @GetMapping("/search")
    public ResponseEntity<List<ArticleResponse>> searchArticles(
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(articleService.searchArticles(q));
    }
}

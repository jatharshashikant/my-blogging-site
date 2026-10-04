package com.blog.controller;

import com.blog.service.LikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for article like/unlike endpoints.
 * Requirements: 6.2, 6.3, 6.5
 */
@RestController
@RequestMapping("/api/articles")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    /**
     * POST /api/articles/{id}/like
     * Increments the like count for the article.
     * Returns 200 with {"likesCount": N}.
     * Returns 404 via GlobalExceptionHandler if the article does not exist.
     * Requirements: 6.2, 6.5
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<Map<String, Integer>> like(@PathVariable Long id) {
        return ResponseEntity.ok(likeService.like(id));
    }

    /**
     * DELETE /api/articles/{id}/like
     * Decrements the like count for the article.
     * Returns 200 with {"likesCount": N}.
     * Returns 400 via GlobalExceptionHandler if the like count is already 0 (ValidationException).
     * Returns 404 via GlobalExceptionHandler if the article does not exist.
     * Requirements: 6.3, 6.5
     */
    @DeleteMapping("/{id}/like")
    public ResponseEntity<Map<String, Integer>> unlike(@PathVariable Long id) {
        return ResponseEntity.ok(likeService.unlike(id));
    }
}

package com.blog.controller;

import com.blog.dto.ArticleRequest;
import com.blog.dto.ArticleResponse;
import com.blog.service.ArticleService;
import com.blog.service.GuestPostService;
import com.blog.service.PdfExtractorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * REST controller for admin-only endpoints.
 * All requests must include valid X-Admin-Key header (enforced by AdminAuthFilter).
 * Requirements: 3.1, 3.3, 3.4, 3.5, 3.6, 3.7, 4.7, 4.8
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:3000")
public class AdminController {

    private final ArticleService articleService;
    private final GuestPostService guestPostService;
    private final PdfExtractorService pdfExtractorService;

    public AdminController(ArticleService articleService,
                          GuestPostService guestPostService,
                          PdfExtractorService pdfExtractorService) {
        this.articleService = articleService;
        this.guestPostService = guestPostService;
        this.pdfExtractorService = pdfExtractorService;
    }

    /**
     * POST /api/admin/articles
     * Create an article directly with title and body.
     * Returns 201 with the created ArticleResponse.
     * Returns 403 if X-Admin-Key is invalid (via AdminAuthFilter).
     * Returns 400 if title is blank (via GlobalExceptionHandler).
     * Requirements: 3.1, 3.3
     */
    @PostMapping("/articles")
    public ResponseEntity<ArticleResponse> createArticle(@RequestBody ArticleRequest req) {
        ArticleResponse created = articleService.createArticle(req, "ADMIN");
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * POST /api/admin/articles/pdf
     * Create an article from a PDF upload.
     * Extracts text from PDF, uses provided title or defaults to filename.
     * Returns 201 with the created ArticleResponse.
     * Returns 400 if PDF is invalid or extraction fails.
     * Requirements: 3.4, 3.5, 3.6, 3.7
     */
    @PostMapping("/articles/pdf")
    public ResponseEntity<ArticleResponse> createArticleFromPdf(
            @RequestPart MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category) {

        // Extract text from PDF
        String body = pdfExtractorService.extract(file);

        // Default title to filename without extension if not provided
        String articleTitle = title;
        if (articleTitle == null || articleTitle.isBlank()) {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && originalFilename.contains(".")) {
                articleTitle = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
            } else {
                articleTitle = "Untitled PDF";
            }
        }

        ArticleRequest req = new ArticleRequest(articleTitle, body, category);
        ArticleResponse created = articleService.createArticle(req, "ADMIN");
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * POST /api/admin/guest-posts/{id}/approve
     * Approve a pending guest post, converting it to a published article.
     * Returns 200 with the resulting ArticleResponse.
     * Returns 404 if guest post doesn't exist.
     * Requirements: 4.7, 4.8
     */
    @PostMapping("/guest-posts/{id}/approve")
    public ResponseEntity<ArticleResponse> approveGuestPost(@PathVariable Long id) {
        ArticleResponse approved = guestPostService.approve(id);
        return ResponseEntity.ok(approved);
    }

    /**
     * GET /api/admin/guest-posts
     * List all pending guest posts awaiting approval.
     * Returns 200 with list of pending guest posts.
     */
    @GetMapping("/guest-posts")
    public ResponseEntity<?> listPendingGuestPosts() {
        var pending = guestPostService.listPending();
        return ResponseEntity.ok(pending);
    }

    /**
     * DELETE /api/admin/articles/{id}
     * Delete a published article.
     * Returns 204 (No Content) on success.
     * Returns 404 if article doesn't exist.
     * Admin-only operation.
     */
    @DeleteMapping("/articles/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }
}

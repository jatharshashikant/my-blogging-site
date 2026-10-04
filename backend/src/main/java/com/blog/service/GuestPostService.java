package com.blog.service;

import com.blog.dto.ArticleResponse;
import com.blog.dto.GuestPostRequest;
import com.blog.exception.ResourceNotFoundException;
import com.blog.exception.ValidationException;
import com.blog.model.Article;
import com.blog.model.GuestPost;
import com.blog.repository.ArticleRepository;
import com.blog.repository.GuestPostRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class GuestPostService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB

    private final GuestPostRepository guestPostRepository;
    private final ArticleRepository articleRepository;

    @Value("${upload.dir}")
    private String uploadDir;

    public GuestPostService(GuestPostRepository guestPostRepository,
                            ArticleRepository articleRepository) {
        this.guestPostRepository = guestPostRepository;
        this.articleRepository = articleRepository;
    }

    /**
     * Validates and submits a guest post, optionally with an attached document.
     * Persists the guest post with status "PENDING".
     */
    public GuestPost submit(GuestPostRequest req, MultipartFile document) {
        // Validate required fields
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new ValidationException("Title must not be blank");
        }
        if (req.getBody() == null || req.getBody().isBlank()) {
            throw new ValidationException("Body must not be blank");
        }

        String documentUrl = null;

        // Validate and save document if provided
        if (document != null && !document.isEmpty()) {
            String contentType = document.getContentType();
            if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
                throw new ValidationException(
                        "Invalid document type. Accepted types: PDF, DOC, DOCX");
            }
            if (document.getSize() > MAX_FILE_SIZE) {
                throw new ValidationException("Document size must not exceed 10 MB");
            }

            documentUrl = saveDocument(document);
        }

        GuestPost guestPost = new GuestPost();
        guestPost.setTitle(req.getTitle());
        guestPost.setBody(req.getBody());
        guestPost.setAuthorName(req.getAuthorName());
        guestPost.setCategory(req.getCategory());
        guestPost.setStatus("PENDING");
        guestPost.setDocumentUrl(documentUrl);

        return guestPostRepository.save(guestPost);
    }

    /**
     * Lists all pending guest posts, ordered by submission date (newest first).
     */
    public java.util.List<GuestPost> listPending() {
        return guestPostRepository.findByStatusOrderBySubmittedAtDesc("PENDING");
    }

    /**
     * Approves a pending guest post: sets status to "APPROVED", creates an Article
     * with source="GUEST", and returns the resulting ArticleResponse.
     */
    @Transactional
    public ArticleResponse approve(Long id) {
        GuestPost guestPost = guestPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "GuestPost not found with id: " + id));

        guestPost.setStatus("APPROVED");
        guestPostRepository.save(guestPost);

        Article article = new Article();
        article.setTitle(guestPost.getTitle());
        article.setBody(guestPost.getBody());
        article.setAuthor(guestPost.getAuthorName() != null && !guestPost.getAuthorName().isBlank()
                ? guestPost.getAuthorName() : "Anonymous");
        article.setCategory(guestPost.getCategory());
        article.setSource("GUEST");
        article.setDocumentUrl(guestPost.getDocumentUrl());

        Article saved = articleRepository.save(article);
        return ArticleResponse.from(saved);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String saveDocument(MultipartFile document) {
        try {
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);

            String originalFilename = document.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            }

            String filename = UUID.randomUUID() + extension;
            Path filePath = uploadPath.resolve(filename);
            Files.write(filePath, document.getBytes());

            // Store relative path (filename only) so the URL is portable
            return filename;
        } catch (IOException e) {
            throw new ValidationException("Failed to save document: " + e.getMessage(), e);
        }
    }
}

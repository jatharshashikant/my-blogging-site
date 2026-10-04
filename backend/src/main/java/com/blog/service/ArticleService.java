package com.blog.service;

import com.blog.dto.ArticleRequest;
import com.blog.dto.ArticleResponse;
import com.blog.exception.ResourceNotFoundException;
import com.blog.exception.ValidationException;
import com.blog.model.Article;
import com.blog.repository.ArticleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ArticleService {

    private final ArticleRepository articleRepository;

    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    /**
     * Returns a paginated list of all articles ordered by publishedAt descending.
     * Requirements: 1.1, 1.4
     */
    public Page<ArticleResponse> listArticles(Pageable pageable) {
        return articleRepository.findAllByOrderByPublishedAtDesc(pageable)
                .map(ArticleResponse::from);
    }

    /**
     * Returns a single article by ID.
     * Throws ResourceNotFoundException if no article with the given ID exists.
     * Requirements: 2.1
     */
    public ArticleResponse getArticle(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Article not found with id: " + id));
        return ArticleResponse.from(article);
    }

    /**
     * Searches articles by keyword across title and body (case-insensitive).
     * Throws ValidationException if the query string is blank.
     * Requirements: 3.1, 3.3
     */
    public List<ArticleResponse> searchArticles(String q) {
        if (q == null || q.isBlank()) {
            throw new ValidationException("Search query must not be blank");
        }
        return articleRepository
                .findByTitleContainingIgnoreCaseOrBodyContainingIgnoreCase(q, q)
                .stream()
                .map(ArticleResponse::from)
                .toList();
    }

    /**
     * Creates and persists a new article.
     * Validates that title is non-blank.
     * The source parameter indicates the origin of the article (e.g. "ADMIN" or "GUEST").
     * Requirements: 5.2, 5.3, 5.5, 9.3
     */
    @Transactional
    public ArticleResponse createArticle(ArticleRequest req, String source) {
        if (req.title() == null || req.title().isBlank()) {
            throw new ValidationException("Article title must not be blank");
        }

        Article article = new Article();
        article.setTitle(req.title().strip());
        article.setBody(req.body() != null ? req.body() : "");
        article.setCategory(req.category());
        article.setSource(source != null ? source : "ADMIN");

        Article saved = articleRepository.save(article);
        return ArticleResponse.from(saved);
    }

    /**
     * Deletes an article by ID.
     * Admin-only operation.
     */
    @Transactional
    public void deleteArticle(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Article not found with id: " + id));
        articleRepository.delete(article);
    }
}

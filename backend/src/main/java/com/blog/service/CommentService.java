package com.blog.service;

import com.blog.dto.CommentRequest;
import com.blog.dto.CommentResponse;
import com.blog.exception.ResourceNotFoundException;
import com.blog.exception.ValidationException;
import com.blog.model.Article;
import com.blog.model.Comment;
import com.blog.repository.ArticleRepository;
import com.blog.repository.CommentRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;

    public CommentService(CommentRepository commentRepository, ArticleRepository articleRepository) {
        this.commentRepository = commentRepository;
        this.articleRepository = articleRepository;
    }

    public CommentResponse addComment(Long articleId, CommentRequest request) {
        if (request.body() == null || request.body().isBlank()) {
            throw new ValidationException("Comment body cannot be empty");
        }
        if (request.authorName() == null || request.authorName().isBlank()) {
            throw new ValidationException("Author name cannot be empty");
        }

        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + articleId));

        Comment comment = new Comment();
        comment.setArticle(article);
        comment.setBody(request.body());
        comment.setAuthorName(request.authorName());

        Comment saved = commentRepository.save(comment);
        return CommentResponse.from(saved);
    }

    public List<CommentResponse> getComments(Long articleId) {
        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + articleId));

        return commentRepository.findByArticleIdOrderByCreatedAtDesc(articleId)
            .stream()
            .map(CommentResponse::from)
            .collect(Collectors.toList());
    }

    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        commentRepository.delete(comment);
    }
}

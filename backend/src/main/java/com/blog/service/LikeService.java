package com.blog.service;

import com.blog.exception.ResourceNotFoundException;
import com.blog.exception.ValidationException;
import com.blog.model.Article;
import com.blog.repository.ArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class LikeService {

    private final ArticleRepository articleRepository;

    public LikeService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    /**
     * Increments the likesCount for the article with the given id.
     *
     * @param id article id
     * @return map containing the updated likesCount
     * @throws ResourceNotFoundException if no article exists with the given id
     */
    @Transactional
    public Map<String, Integer> like(Long id) {
        Article article = findArticleOrThrow(id);
        article.setLikesCount(article.getLikesCount() + 1);
        articleRepository.save(article);
        return Map.of("likesCount", article.getLikesCount());
    }

    /**
     * Decrements the likesCount for the article with the given id.
     *
     * @param id article id
     * @return map containing the updated likesCount
     * @throws ResourceNotFoundException if no article exists with the given id
     * @throws ValidationException       if the article's likesCount is already 0
     */
    @Transactional
    public Map<String, Integer> unlike(Long id) {
        Article article = findArticleOrThrow(id);
        if (article.getLikesCount() == 0) {
            throw new ValidationException("Like count is already 0");
        }
        article.setLikesCount(article.getLikesCount() - 1);
        articleRepository.save(article);
        return Map.of("likesCount", article.getLikesCount());
    }

    private Article findArticleOrThrow(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + id));
    }
}

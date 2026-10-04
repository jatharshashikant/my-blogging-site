package com.blog.service;

import com.blog.dto.ReactionCountsResponse;
import com.blog.exception.ResourceNotFoundException;
import com.blog.exception.ValidationException;
import com.blog.model.Article;
import com.blog.model.Reaction;
import com.blog.repository.ArticleRepository;
import com.blog.repository.ReactionRepository;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ReactionService {
    private final ReactionRepository reactionRepository;
    private final ArticleRepository articleRepository;

    public ReactionService(ReactionRepository reactionRepository, ArticleRepository articleRepository) {
        this.reactionRepository = reactionRepository;
        this.articleRepository = articleRepository;
    }

    public ReactionCountsResponse addReaction(Long articleId, String sessionId, String reactionType) {
        if (!isValidReactionType(reactionType)) {
            throw new ValidationException("Invalid reaction type. Allowed: LOVE, LIKE, DISLIKE");
        }

        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + articleId));

        // Check if user already has this reaction
        Optional<Reaction> existing = reactionRepository.findByArticleIdAndSessionIdAndReactionType(articleId, sessionId, reactionType);
        if (existing.isPresent()) {
            // Remove if already exists (toggle)
            reactionRepository.delete(existing.get());
        } else {
            // Remove any other reaction from this user
            List<Reaction> otherReactions = reactionRepository.findByArticleId(articleId);
            for (Reaction r : otherReactions) {
                if (r.getSessionId().equals(sessionId) && !r.getReactionType().equals(reactionType)) {
                    reactionRepository.delete(r);
                }
            }

            // Add new reaction
            Reaction reaction = new Reaction();
            reaction.setArticle(article);
            reaction.setSessionId(sessionId);
            reaction.setReactionType(reactionType);
            reactionRepository.save(reaction);
        }

        return getReactionCounts(articleId, sessionId);
    }

    public ReactionCountsResponse getReactionCounts(Long articleId, String sessionId) {
        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + articleId));

        List<Reaction> reactions = reactionRepository.findByArticleId(articleId);

        long loveCount = reactions.stream().filter(r -> "LOVE".equals(r.getReactionType())).count();
        long likeCount = reactions.stream().filter(r -> "LIKE".equals(r.getReactionType())).count();
        long dislikeCount = reactions.stream().filter(r -> "DISLIKE".equals(r.getReactionType())).count();

        String userReaction = reactions.stream()
            .filter(r -> r.getSessionId().equals(sessionId))
            .map(Reaction::getReactionType)
            .findFirst()
            .orElse(null);

        return new ReactionCountsResponse(loveCount, likeCount, dislikeCount, userReaction);
    }

    private boolean isValidReactionType(String type) {
        return "LOVE".equals(type) || "LIKE".equals(type) || "DISLIKE".equals(type);
    }
}

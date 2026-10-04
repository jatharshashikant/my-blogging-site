package com.blog.repository;

import com.blog.model.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.List;
import java.util.Map;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    List<Reaction> findByArticleId(Long articleId);
    
    Optional<Reaction> findByArticleIdAndSessionIdAndReactionType(Long articleId, String sessionId, String reactionType);
    
    @Query("SELECT r.reactionType, COUNT(r) FROM Reaction r WHERE r.article.id = ?1 GROUP BY r.reactionType")
    List<Object[]> getReactionCounts(Long articleId);
}

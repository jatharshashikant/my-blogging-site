package com.blog.controller;

import com.blog.dto.ReactionCountsResponse;
import com.blog.service.ReactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

@RestController
@RequestMapping("/api/articles/{articleId}/reactions")
@CrossOrigin(origins = "http://localhost:3000")
public class ReactionController {
    private final ReactionService reactionService;

    public ReactionController(ReactionService reactionService) {
        this.reactionService = reactionService;
    }

    @PostMapping("/{reactionType}")
    public ResponseEntity<ReactionCountsResponse> addReaction(
            @PathVariable Long articleId,
            @PathVariable String reactionType,
            HttpServletRequest request) {
        
        String sessionId = getOrCreateSessionId(request);
        ReactionCountsResponse response = reactionService.addReaction(articleId, sessionId, reactionType);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ReactionCountsResponse> getReactions(
            @PathVariable Long articleId,
            HttpServletRequest request) {
        
        String sessionId = getOrCreateSessionId(request);
        ReactionCountsResponse response = reactionService.getReactionCounts(articleId, sessionId);
        return ResponseEntity.ok(response);
    }

    private String getOrCreateSessionId(HttpServletRequest request) {
        // Get or create a session ID from cookie
        String sessionId = null;
        
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("sessionId".equals(cookie.getName())) {
                    sessionId = cookie.getValue();
                    break;
                }
            }
        }

        // If no session ID, create one
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
        }

        return sessionId;
    }
}

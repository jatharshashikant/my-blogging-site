package com.blog.controller;

import com.blog.model.Article;
import com.blog.repository.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class LikeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArticleRepository articleRepository;

    private Article testArticle;

    @BeforeEach
    void setUp() {
        testArticle = new Article();
        testArticle.setTitle("Test Article");
        testArticle.setBody("Test body");
        testArticle.setAuthor("Test Author");
        testArticle.setLikesCount(0);
        testArticle.setPublishedAt(LocalDateTime.now());
        testArticle = articleRepository.save(testArticle);
    }

    // ==================== POST /api/articles/{id}/like Tests ====================

    @Test
    @DisplayName("POST /api/articles/{id}/like: increments like count and returns 200")
    void testLikeIncrementsCount() throws Exception {
        mockMvc.perform(post("/api/articles/" + testArticle.getId() + "/like"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.likesCount", is(1)));
        
        // Verify persistence
        Article updated = articleRepository.findById(testArticle.getId()).orElseThrow();
        assert updated.getLikesCount() == 1;
    }

    @Test
    @DisplayName("POST /api/articles/{id}/like: increments multiple times")
    void testLikesMultipleTimes() throws Exception {
        mockMvc.perform(post("/api/articles/" + testArticle.getId() + "/like"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.likesCount", is(1)));
        
        mockMvc.perform(post("/api/articles/" + testArticle.getId() + "/like"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.likesCount", is(2)));
        
        Article updated = articleRepository.findById(testArticle.getId()).orElseThrow();
        assert updated.getLikesCount() == 2;
    }

    @Test
    @DisplayName("POST /api/articles/{id}/like: returns 404 for unknown article")
    void testLikeNotFound() throws Exception {
        mockMvc.perform(post("/api/articles/99999/like"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error", is("NOT_FOUND")))
            .andExpect(jsonPath("$.message").exists());
    }

    // ==================== DELETE /api/articles/{id}/like Tests ====================

    @Test
    @DisplayName("DELETE /api/articles/{id}/like: decrements like count and returns 200")
    void testUnlikeDecrementsCount() throws Exception {
        // First, like the article
        testArticle.setLikesCount(5);
        testArticle = articleRepository.save(testArticle);
        
        mockMvc.perform(delete("/api/articles/" + testArticle.getId() + "/like"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.likesCount", is(4)));
        
        // Verify persistence
        Article updated = articleRepository.findById(testArticle.getId()).orElseThrow();
        assert updated.getLikesCount() == 4;
    }

    @Test
    @DisplayName("DELETE /api/articles/{id}/like: decrements multiple times")
    void testUnlikeMultipleTimes() throws Exception {
        // Set initial count
        testArticle.setLikesCount(3);
        testArticle = articleRepository.save(testArticle);
        
        mockMvc.perform(delete("/api/articles/" + testArticle.getId() + "/like"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.likesCount", is(2)));
        
        mockMvc.perform(delete("/api/articles/" + testArticle.getId() + "/like"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.likesCount", is(1)));
        
        Article updated = articleRepository.findById(testArticle.getId()).orElseThrow();
        assert updated.getLikesCount() == 1;
    }

    @Test
    @DisplayName("DELETE /api/articles/{id}/like: returns 400 when count is already 0")
    void testUnlikeWhenZero() throws Exception {
        mockMvc.perform(delete("/api/articles/" + testArticle.getId() + "/like"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("DELETE /api/articles/{id}/like: returns 404 for unknown article")
    void testUnlikeNotFound() throws Exception {
        mockMvc.perform(delete("/api/articles/99999/like"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error", is("NOT_FOUND")))
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("DELETE /api/articles/{id}/like: does not decrement when at 0 (idempotency check)")
    void testUnlikeIdempotency() throws Exception {
        // Try to unlike when count is 0
        mockMvc.perform(delete("/api/articles/" + testArticle.getId() + "/like"))
            .andExpect(status().isBadRequest());
        
        // Try again - should still fail
        mockMvc.perform(delete("/api/articles/" + testArticle.getId() + "/like"))
            .andExpect(status().isBadRequest());
        
        // Verify count is still 0
        Article updated = articleRepository.findById(testArticle.getId()).orElseThrow();
        assert updated.getLikesCount() == 0;
    }
}

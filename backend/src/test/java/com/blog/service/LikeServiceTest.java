package com.blog.service;

import com.blog.exception.ResourceNotFoundException;
import com.blog.exception.ValidationException;
import com.blog.model.Article;
import com.blog.repository.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LikeServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @InjectMocks
    private LikeService likeService;

    private Article testArticle;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testArticle = new Article();
        testArticle.setId(1L);
        testArticle.setTitle("Test Article");
        testArticle.setBody("Test body");
        testArticle.setAuthor("Test Author");
        testArticle.setLikesCount(5);
        testArticle.setPublishedAt(LocalDateTime.now());
    }

    // ==================== like() Tests ====================

    @Test
    @DisplayName("like: increments likesCount by 1")
    void testLikeIncrementsCount() {
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));
        when(articleRepository.save(any(Article.class))).thenReturn(testArticle);

        Map<String, Integer> result = likeService.like(1L);

        assertEquals(6, result.get("likesCount"));
        assertEquals(6, testArticle.getLikesCount());
        verify(articleRepository, times(1)).save(testArticle);
    }

    @Test
    @DisplayName("like: increments from 0 to 1")
    void testLikeIncrementsFromZero() {
        testArticle.setLikesCount(0);
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));
        when(articleRepository.save(any(Article.class))).thenReturn(testArticle);

        Map<String, Integer> result = likeService.like(1L);

        assertEquals(1, result.get("likesCount"));
        verify(articleRepository, times(1)).save(testArticle);
    }

    @Test
    @DisplayName("like: throws ResourceNotFoundException for unknown article")
    void testLikeThrowsNotFound() {
        when(articleRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
            () -> likeService.like(999L));
        
        assertTrue(ex.getMessage().contains("Article not found"));
    }

    // ==================== unlike() Tests ====================

    @Test
    @DisplayName("unlike: decrements likesCount by 1")
    void testUnlikeDecrementsCount() {
        testArticle.setLikesCount(5);
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));
        when(articleRepository.save(any(Article.class))).thenReturn(testArticle);

        Map<String, Integer> result = likeService.unlike(1L);

        assertEquals(4, result.get("likesCount"));
        assertEquals(4, testArticle.getLikesCount());
        verify(articleRepository, times(1)).save(testArticle);
    }

    @Test
    @DisplayName("unlike: decrements from 1 to 0")
    void testUnlikeDecrementsToZero() {
        testArticle.setLikesCount(1);
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));
        when(articleRepository.save(any(Article.class))).thenReturn(testArticle);

        Map<String, Integer> result = likeService.unlike(1L);

        assertEquals(0, result.get("likesCount"));
        verify(articleRepository, times(1)).save(testArticle);
    }

    @Test
    @DisplayName("unlike: throws ValidationException when count is 0")
    void testUnlikeThrowsWhenZero() {
        testArticle.setLikesCount(0);
        when(articleRepository.findById(1L)).thenReturn(Optional.of(testArticle));

        ValidationException ex = assertThrows(ValidationException.class,
            () -> likeService.unlike(1L));
        
        assertTrue(ex.getMessage().contains("Like count is already 0"));
        verify(articleRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("unlike: throws ResourceNotFoundException for unknown article")
    void testUnlikeThrowsNotFound() {
        when(articleRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
            () -> likeService.unlike(999L));
        
        assertTrue(ex.getMessage().contains("Article not found"));
    }
}

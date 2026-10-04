package com.blog;

import com.blog.dto.ArticleRequest;
import com.blog.model.Article;
import com.blog.repository.ArticleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ArticleController.
 * Validates all endpoints work correctly with the service layer and database.
 * Requirements: 1.1, 1.4, 2.1, 5.2, 5.3, 5.5
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DisplayName("ArticleController Integration Tests")
class ArticleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        articleRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/articles returns paginated list with default size 10")
    void testListArticlesDefaultPagination() throws Exception {
        // Create 15 articles to exceed the default page size
        for (int i = 1; i <= 15; i++) {
            Article article = new Article();
            article.setTitle("Article " + i);
            article.setBody("Body " + i);
            article.setAuthor("Admin");
            article.setPublishedAt(LocalDateTime.now().minusDays(15 - i));
            articleRepository.save(article);
        }

        mockMvc.perform(get("/api/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.totalElements", equalTo(15)))
                .andExpect(jsonPath("$.totalPages", equalTo(2)))
                .andExpect(jsonPath("$.size", equalTo(10)));
    }

    @Test
    @DisplayName("GET /api/articles?page=1 returns second page")
    void testListArticlesSecondPage() throws Exception {
        // Create 15 articles
        for (int i = 1; i <= 15; i++) {
            Article article = new Article();
            article.setTitle("Article " + i);
            article.setBody("Body " + i);
            article.setAuthor("Admin");
            article.setPublishedAt(LocalDateTime.now().minusDays(15 - i));
            articleRepository.save(article);
        }

        mockMvc.perform(get("/api/articles?page=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements", equalTo(15)))
                .andExpect(jsonPath("$.number", equalTo(1)));
    }

    @Test
    @DisplayName("GET /api/articles returns ordered by publishedAt descending")
    void testListArticlesOrderedByPublishedAtDescending() throws Exception {
        // Create articles with specific dates
        Article article1 = new Article();
        article1.setTitle("Article 1");
        article1.setBody("Body 1");
        article1.setAuthor("Admin");
        article1.setPublishedAt(LocalDateTime.of(2024, 1, 1, 10, 0));
        articleRepository.save(article1);

        Article article2 = new Article();
        article2.setTitle("Article 2");
        article2.setBody("Body 2");
        article2.setAuthor("Admin");
        article2.setPublishedAt(LocalDateTime.of(2024, 1, 3, 10, 0));
        articleRepository.save(article2);

        Article article3 = new Article();
        article3.setTitle("Article 3");
        article3.setBody("Body 3");
        article3.setAuthor("Admin");
        article3.setPublishedAt(LocalDateTime.of(2024, 1, 2, 10, 0));
        articleRepository.save(article3);

        mockMvc.perform(get("/api/articles?size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title", equalTo("Article 2")))
                .andExpect(jsonPath("$.content[1].title", equalTo("Article 3")))
                .andExpect(jsonPath("$.content[2].title", equalTo("Article 1")));
    }

    @Test
    @DisplayName("GET /api/articles/{id} returns single article")
    void testGetArticleById() throws Exception {
        Article article = new Article();
        article.setTitle("Test Article");
        article.setBody("Test Body");
        article.setAuthor("Admin");
        article.setCategory("Tech");
        Article saved = articleRepository.save(article);

        mockMvc.perform(get("/api/articles/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(saved.getId().intValue())))
                .andExpect(jsonPath("$.title", equalTo("Test Article")))
                .andExpect(jsonPath("$.body", equalTo("Test Body")))
                .andExpect(jsonPath("$.author", equalTo("Admin")))
                .andExpect(jsonPath("$.category", equalTo("Tech")))
                .andExpect(jsonPath("$.likesCount", equalTo(0)));
    }

    @Test
    @DisplayName("GET /api/articles/{id} returns 404 for non-existent article")
    void testGetArticleByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/articles/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", equalTo("NOT_FOUND")))
                .andExpect(jsonPath("$.message", containsString("Article not found")));
    }

    @Test
    @DisplayName("GET /api/articles/search?q=keyword returns matching articles")
    void testSearchArticles() throws Exception {
        Article article1 = new Article();
        article1.setTitle("Spring Boot Tutorial");
        article1.setBody("Learn Spring Boot");
        article1.setAuthor("Admin");
        articleRepository.save(article1);

        Article article2 = new Article();
        article2.setTitle("Java Basics");
        article2.setBody("Understanding Java and Spring");
        article2.setAuthor("Admin");
        articleRepository.save(article2);

        Article article3 = new Article();
        article3.setTitle("React Guide");
        article3.setBody("Frontend development");
        article3.setAuthor("Admin");
        articleRepository.save(article3);

        mockMvc.perform(get("/api/articles/search?q=Spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].title", hasItems("Spring Boot Tutorial", "Java Basics")));
    }

    @Test
    @DisplayName("GET /api/articles/search?q=keyword is case-insensitive")
    void testSearchArticlesCaseInsensitive() throws Exception {
        Article article = new Article();
        article.setTitle("Spring Boot Tutorial");
        article.setBody("Learn Spring Boot");
        article.setAuthor("Admin");
        articleRepository.save(article);

        mockMvc.perform(get("/api/articles/search?q=spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", equalTo("Spring Boot Tutorial")));
    }

    @Test
    @DisplayName("GET /api/articles/search without q parameter returns 400")
    void testSearchArticlesWithoutQuery() throws Exception {
        mockMvc.perform(get("/api/articles/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", equalTo("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.message", containsString("blank")));
    }

    @Test
    @DisplayName("GET /api/articles/search with empty q parameter returns 400")
    void testSearchArticlesWithEmptyQuery() throws Exception {
        mockMvc.perform(get("/api/articles/search?q="))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", equalTo("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.message", containsString("blank")));
    }

    @Test
    @DisplayName("GET /api/articles/search returns no results for non-matching query")
    void testSearchArticlesNoResults() throws Exception {
        Article article = new Article();
        article.setTitle("Spring Boot Tutorial");
        article.setBody("Learn Spring Boot");
        article.setAuthor("Admin");
        articleRepository.save(article);

        mockMvc.perform(get("/api/articles/search?q=Nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}

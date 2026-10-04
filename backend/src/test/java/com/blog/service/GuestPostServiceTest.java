package com.blog.service;

import com.blog.dto.ArticleResponse;
import com.blog.dto.GuestPostRequest;
import com.blog.exception.ResourceNotFoundException;
import com.blog.exception.ValidationException;
import com.blog.model.Article;
import com.blog.model.GuestPost;
import com.blog.repository.ArticleRepository;
import com.blog.repository.GuestPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GuestPostServiceTest {

    @Mock
    private GuestPostRepository guestPostRepository;

    @Mock
    private ArticleRepository articleRepository;

    @InjectMocks
    private GuestPostService guestPostService;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Set the upload directory to temp directory for testing
        ReflectionTestUtils.setField(guestPostService, "uploadDir", tempDir.toString());
    }

    // ==================== submit() Tests ====================

    @Test
    @DisplayName("submit: rejects blank title")
    void testSubmitRejectsBlankTitle() {
        GuestPostRequest req = new GuestPostRequest("", "Valid body", "Author", "Tech");
        
        ValidationException ex = assertThrows(ValidationException.class, 
            () -> guestPostService.submit(req, null));
        
        assertTrue(ex.getMessage().contains("Title must not be blank"));
        verify(guestPostRepository, never()).save(any());
    }

    @Test
    @DisplayName("submit: rejects null title")
    void testSubmitRejectsNullTitle() {
        GuestPostRequest req = new GuestPostRequest(null, "Valid body", "Author", "Tech");
        
        ValidationException ex = assertThrows(ValidationException.class, 
            () -> guestPostService.submit(req, null));
        
        assertTrue(ex.getMessage().contains("Title must not be blank"));
        verify(guestPostRepository, never()).save(any());
    }

    @Test
    @DisplayName("submit: rejects blank body")
    void testSubmitRejectsBlankBody() {
        GuestPostRequest req = new GuestPostRequest("Valid title", "  ", "Author", "Tech");
        
        ValidationException ex = assertThrows(ValidationException.class, 
            () -> guestPostService.submit(req, null));
        
        assertTrue(ex.getMessage().contains("Body must not be blank"));
        verify(guestPostRepository, never()).save(any());
    }

    @Test
    @DisplayName("submit: rejects null body")
    void testSubmitRejectsNullBody() {
        GuestPostRequest req = new GuestPostRequest("Valid title", null, "Author", "Tech");
        
        ValidationException ex = assertThrows(ValidationException.class, 
            () -> guestPostService.submit(req, null));
        
        assertTrue(ex.getMessage().contains("Body must not be blank"));
        verify(guestPostRepository, never()).save(any());
    }

    @Test
    @DisplayName("submit: rejects unsupported MIME type")
    void testSubmitRejectsUnsupportedMimeType() throws IOException {
        GuestPostRequest req = new GuestPostRequest("Title", "Body", "Author", "Tech");
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("application/json"); // Unsupported
        when(file.getSize()).thenReturn(1000L);

        ValidationException ex = assertThrows(ValidationException.class, 
            () -> guestPostService.submit(req, file));
        
        assertTrue(ex.getMessage().contains("Invalid document type"));
        verify(guestPostRepository, never()).save(any());
    }

    @Test
    @DisplayName("submit: rejects file exceeding 10 MB")
    void testSubmitRejectsFileSizeExceedingLimit() throws IOException {
        GuestPostRequest req = new GuestPostRequest("Title", "Body", "Author", "Tech");
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(11L * 1024 * 1024); // 11 MB

        ValidationException ex = assertThrows(ValidationException.class, 
            () -> guestPostService.submit(req, file));
        
        assertTrue(ex.getMessage().contains("10 MB"));
        verify(guestPostRepository, never()).save(any());
    }

    @Test
    @DisplayName("submit: accepts valid request without document")
    void testSubmitValidRequestWithoutDocument() {
        GuestPostRequest req = new GuestPostRequest("Test Title", "Test Body", "John Doe", "Tech");
        GuestPost savedPost = new GuestPost();
        savedPost.setId(1L);
        savedPost.setTitle("Test Title");
        savedPost.setBody("Test Body");
        savedPost.setAuthorName("John Doe");
        savedPost.setCategory("Tech");
        savedPost.setStatus("PENDING");
        
        when(guestPostRepository.save(any(GuestPost.class))).thenReturn(savedPost);

        GuestPost result = guestPostService.submit(req, null);

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        assertEquals("Test Title", result.getTitle());
        assertEquals("Test Body", result.getBody());
        assertEquals("John Doe", result.getAuthorName());
        
        ArgumentCaptor<GuestPost> captor = ArgumentCaptor.forClass(GuestPost.class);
        verify(guestPostRepository).save(captor.capture());
        GuestPost saved = captor.getValue();
        assertEquals("PENDING", saved.getStatus());
    }

    @Test
    @DisplayName("submit: accepts empty document")
    void testSubmitAcceptsEmptyDocument() {
        GuestPostRequest req = new GuestPostRequest("Title", "Body", "Author", "Tech");
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);
        
        GuestPost savedPost = new GuestPost();
        savedPost.setId(1L);
        savedPost.setStatus("PENDING");
        savedPost.setDocumentUrl(null);
        
        when(guestPostRepository.save(any(GuestPost.class))).thenReturn(savedPost);

        GuestPost result = guestPostService.submit(req, file);

        assertNotNull(result);
        assertNull(result.getDocumentUrl());
        verify(guestPostRepository).save(any());
    }

    @Test
    @DisplayName("submit: accepts valid PDF document")
    void testSubmitAcceptsValidPdfDocument() throws IOException {
        GuestPostRequest req = new GuestPostRequest("Title", "Body", "Author", "Tech");
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(5L * 1024 * 1024); // 5 MB
        when(file.getBytes()).thenReturn(new byte[]{1, 2, 3}); // minimal content
        when(file.getOriginalFilename()).thenReturn("document.pdf");
        
        GuestPost savedPost = new GuestPost();
        savedPost.setId(1L);
        savedPost.setStatus("PENDING");
        savedPost.setDocumentUrl("some-uuid.pdf");
        
        when(guestPostRepository.save(any(GuestPost.class))).thenReturn(savedPost);

        GuestPost result = guestPostService.submit(req, file);

        assertNotNull(result);
        assertNotNull(result.getDocumentUrl());
        verify(guestPostRepository).save(any());
    }

    @Test
    @DisplayName("submit: accepts valid DOCX document")
    void testSubmitAcceptsValidDocxDocument() throws IOException {
        GuestPostRequest req = new GuestPostRequest("Title", "Body", "Author", "Tech");
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        when(file.getSize()).thenReturn(1L * 1024 * 1024); // 1 MB
        when(file.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(file.getOriginalFilename()).thenReturn("document.docx");
        
        GuestPost savedPost = new GuestPost();
        savedPost.setId(1L);
        savedPost.setStatus("PENDING");
        savedPost.setDocumentUrl("some-uuid.docx");
        
        when(guestPostRepository.save(any(GuestPost.class))).thenReturn(savedPost);

        GuestPost result = guestPostService.submit(req, file);

        assertNotNull(result);
        assertNotNull(result.getDocumentUrl());
        verify(guestPostRepository).save(any());
    }

    // ==================== approve() Tests ====================

    @Test
    @DisplayName("approve: throws ResourceNotFoundException for unknown id")
    void testApproveThrowsNotFoundForUnknownId() {
        when(guestPostRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
            () -> guestPostService.approve(999L));
        
        assertTrue(ex.getMessage().contains("GuestPost not found"));
        verify(articleRepository, never()).save(any());
    }

    @Test
    @DisplayName("approve: sets status to APPROVED and creates Article")
    void testApproveCreatesArticleFromGuestPost() {
        // Setup guest post
        GuestPost guestPost = new GuestPost();
        guestPost.setId(1L);
        guestPost.setTitle("Guest Article Title");
        guestPost.setBody("Guest article body content");
        guestPost.setAuthorName("Jane Doe");
        guestPost.setCategory("Politics");
        guestPost.setStatus("PENDING");
        guestPost.setDocumentUrl("document.pdf");
        
        when(guestPostRepository.findById(1L)).thenReturn(Optional.of(guestPost));
        
        // Setup saved article
        Article savedArticle = new Article();
        savedArticle.setId(100L);
        savedArticle.setTitle("Guest Article Title");
        savedArticle.setBody("Guest article body content");
        savedArticle.setAuthor("Jane Doe");
        savedArticle.setCategory("Politics");
        savedArticle.setSource("GUEST");
        savedArticle.setDocumentUrl("document.pdf");
        savedArticle.setLikesCount(0);
        
        when(articleRepository.save(any(Article.class))).thenReturn(savedArticle);

        ArticleResponse result = guestPostService.approve(1L);

        assertNotNull(result);
        assertEquals(100L, result.id());
        assertEquals("Guest Article Title", result.title());
        assertEquals("Jane Doe", result.author());
        assertEquals("Politics", result.category());
        assertEquals("document.pdf", result.documentUrl());
        
        // Verify guest post was marked APPROVED
        ArgumentCaptor<GuestPost> guestPostCaptor = ArgumentCaptor.forClass(GuestPost.class);
        verify(guestPostRepository).save(guestPostCaptor.capture());
        assertEquals("APPROVED", guestPostCaptor.getValue().getStatus());
        
        // Verify article was saved
        ArgumentCaptor<Article> articleCaptor = ArgumentCaptor.forClass(Article.class);
        verify(articleRepository).save(articleCaptor.capture());
        Article savedArt = articleCaptor.getValue();
        assertEquals("GUEST", savedArt.getSource());
    }

    @Test
    @DisplayName("approve: sets author to 'Anonymous' when authorName is blank")
    void testApproveUsesAnonymousWhenAuthorBlank() {
        GuestPost guestPost = new GuestPost();
        guestPost.setId(1L);
        guestPost.setTitle("Title");
        guestPost.setBody("Body");
        guestPost.setAuthorName(""); // blank
        guestPost.setStatus("PENDING");
        
        when(guestPostRepository.findById(1L)).thenReturn(Optional.of(guestPost));
        
        Article savedArticle = new Article();
        savedArticle.setId(100L);
        savedArticle.setAuthor("Anonymous");
        
        when(articleRepository.save(any(Article.class))).thenReturn(savedArticle);

        ArticleResponse result = guestPostService.approve(1L);

        assertEquals("Anonymous", result.author());
    }

    @Test
    @DisplayName("approve: sets author to 'Anonymous' when authorName is null")
    void testApproveUsesAnonymousWhenAuthorNull() {
        GuestPost guestPost = new GuestPost();
        guestPost.setId(1L);
        guestPost.setTitle("Title");
        guestPost.setBody("Body");
        guestPost.setAuthorName(null);
        guestPost.setStatus("PENDING");
        
        when(guestPostRepository.findById(1L)).thenReturn(Optional.of(guestPost));
        
        Article savedArticle = new Article();
        savedArticle.setId(100L);
        savedArticle.setAuthor("Anonymous");
        
        when(articleRepository.save(any(Article.class))).thenReturn(savedArticle);

        ArticleResponse result = guestPostService.approve(1L);

        assertEquals("Anonymous", result.author());
    }

    @Test
    @DisplayName("approve: marked as @Transactional")
    void testApproveIsTransactional() throws NoSuchMethodException {
        var method = guestPostService.getClass().getMethod("approve", Long.class);
        assertTrue(method.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class),
            "approve() method should be annotated with @Transactional");
    }
}

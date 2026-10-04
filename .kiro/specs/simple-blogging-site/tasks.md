# Implementation Plan: simple-blogging-site

## Overview

Incrementally build a two-tier blogging application: a Spring Boot REST API (backend) and a
React SPA (frontend). Tasks follow dependency order — shared foundations first, then services,
then controllers, then frontend scaffold, components, pages, and finally wiring/config files.
Property-based tests (jqwik) are placed immediately after the code they validate.

---

## Tasks

- [x] 1. Backend project scaffold
  - [x] 1.1 Create Maven project structure and pom.xml
    - Generate `backend/` directory with standard Maven layout (`src/main/java`, `src/main/resources`, `src/test/java`).
    - Write `pom.xml` with Spring Boot parent (`3.x`), dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `h2` (scope `runtime`), `postgresql` (scope `runtime`), `pdfbox:3.x`, `jqwik`, `spring-boot-starter-test`, `testcontainers`.
    - Create `com.blog.BlogApplication` main class with `@SpringBootApplication`.
    - _Requirements: 10.2, 10.4_

  - [x] 1.2 Create application properties files
    - Write `src/main/resources/application.properties` with common settings: `spring.application.name`, `server.port=8080`, `spring.servlet.multipart.max-file-size=10MB`, `spring.servlet.multipart.max-request-size=10MB`.
    - Write `src/main/resources/application-dev.properties` with H2 in-memory datasource, `spring.jpa.hibernate.ddl-auto=create-drop`, `allowed.origins=http://localhost:3000`, `admin.key=dev-admin-key`, `upload.dir=uploads`.
    - Write `src/main/resources/application-prod.properties` with `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`, `${ALLOWED_ORIGINS}`, `${ADMIN_KEY}`, `${UPLOAD_DIR:/app/uploads}`, `spring.jpa.hibernate.ddl-auto=update`.
    - _Requirements: 10.3, 10.4_

- [x] 2. JPA entities and repositories
  - [x] 2.1 Implement `Article` JPA entity
    - Create `com.blog.model.Article` with `@Entity @Table(name="articles")`.
    - Fields: `id` (`@Id @GeneratedValue IDENTITY`), `title` (`VARCHAR(500) NOT NULL`), `body` (`TEXT NOT NULL DEFAULT ''`), `author` (`VARCHAR(255) NOT NULL DEFAULT 'Admin'`), `category`, `likesCount` (`int DEFAULT 0`), `source` (`VARCHAR(20) DEFAULT 'ADMIN'`), `documentUrl`, `publishedAt` (`LocalDateTime`).
    - Add `@PrePersist` to set `publishedAt = LocalDateTime.now()` if null.
    - Include getters, setters, and a no-arg constructor (or use Lombok `@Data`).
    - _Requirements: 1.2, 2.1, 3.3_

  - [x] 2.2 Implement `GuestPost` JPA entity
    - Create `com.blog.model.GuestPost` with `@Entity @Table(name="guest_posts")`.
    - Fields: `id`, `title`, `body` (`TEXT NOT NULL`), `authorName`, `category`, `status` (`VARCHAR(20) DEFAULT 'PENDING'`), `documentUrl`, `submittedAt` (`LocalDateTime`).
    - Add `@PrePersist` to set `submittedAt`.
    - _Requirements: 4.3, 4.7_

  - [x] 2.3 Implement `ArticleRepository`
    - Create `com.blog.repository.ArticleRepository` extending `JpaRepository<Article, Long>`.
    - Add `Page<Article> findAllByOrderByPublishedAtDesc(Pageable pageable)`.
    - Add `List<Article> findByTitleContainingIgnoreCaseOrBodyContainingIgnoreCase(String titleKw, String bodyKw)`.
    - _Requirements: 1.1, 1.4, 5.3_

  - [x] 2.4 Implement `GuestPostRepository`
    - Create `com.blog.repository.GuestPostRepository` extending `JpaRepository<GuestPost, Long>`.
    - _Requirements: 4.3, 4.7_

- [x] 3. DTOs and exceptions
  - [x] 3.1 Implement DTO classes
    - Create `com.blog.dto.ArticleRequest` record: `String title`, `String body`, `String category`.
    - Create `com.blog.dto.ArticleResponse` record: `Long id`, `String title`, `String body`, `String author`, `String category`, `int likesCount`, `String documentUrl`, `LocalDateTime publishedAt`.
    - Add a static factory `ArticleResponse.from(Article a)`.
    - Create `com.blog.dto.GuestPostRequest` — fields mirroring the multipart form (`title`, `body`, `authorName`, `category`; file handled separately as `MultipartFile`).
    - Create `com.blog.dto.ErrorResponse` record: `String error`, `String message`.
    - _Requirements: 9.4_

  - [x] 3.2 Implement custom exceptions
    - Create `com.blog.exception.ResourceNotFoundException` extending `RuntimeException`.
    - Create `com.blog.exception.ValidationException` extending `RuntimeException`.
    - _Requirements: 9.1, 9.3_

  - [x] 3.3 Implement `GlobalExceptionHandler`
    - Create `com.blog.exception.GlobalExceptionHandler` annotated `@RestControllerAdvice`.
    - Map `ResourceNotFoundException` → 404 `ErrorResponse("NOT_FOUND", …)`.
    - Map `ValidationException` → 400 `ErrorResponse("VALIDATION_ERROR", …)`.
    - Map `Exception` → 500 `ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred")`.
    - _Requirements: 9.1, 9.2, 9.3, 9.4_

  - [x]* 3.4 Write property test for error response schema (Property 10)
    - **Property 10: All error responses conform to the standard schema**
    - **Validates: Requirements 9.1, 9.2, 9.4**
    - Test class: `ErrorResponseSchemaPropertyTest` using jqwik `@Property`.
    - Generate various invalid requests; assert every 4xx/5xx body has non-empty `error` and `message` fields.

- [x] 4. Security and CORS configuration
  - [x] 4.1 Implement `CorsConfig` bean
    - Create `com.blog.config.CorsConfig` `@Configuration` class.
    - Inject `${allowed.origins}` as `String[]`.
    - Expose `CorsConfigurationSource` bean allowing `GET`, `POST`, `DELETE`, `OPTIONS` and all headers.
    - _Requirements: 10.1, 10.5_

  - [x] 4.2 Implement `AdminAuthFilter`
    - Create `com.blog.filter.AdminAuthFilter` extending `OncePerRequestFilter`.
    - Apply only to requests matching `/api/admin/**`.
    - Compare `X-Admin-Key` header value to `${admin.key}` property.
    - Return 403 `ErrorResponse` immediately if missing or mismatched.
    - _Requirements: 3.2_

  - [x]* 4.3 Write property test for admin auth rejection (Property 4)
    - **Property 4: Admin endpoints reject any request without a valid key**
    - **Validates: Requirements 3.2**
    - Test class: `AdminAuthPropertyTest`.
    - For arbitrary strings that are not the configured secret, assert every `/api/admin/**` request returns HTTP 403.

- [x] 5. PDF extraction service
  - [x] 5.1 Implement `PdfExtractorService`
    - Create `com.blog.service.PdfExtractorService` `@Service`.
    - Implement `public String extract(MultipartFile file) throws IOException`.
    - Use `Loader.loadPDF(file.getBytes())`, `PDFTextStripper` with `setSortByPosition(true)`, `setLineSeparator("\n")`, `setParagraphStart("\n")`.
    - Return `.strip()` of extracted text; return `""` if blank.
    - Throw `ValidationException` if `file.getContentType()` is not `application/pdf`.
    - _Requirements: 3.4, 3.5, 3.6, 8.1, 8.3_

  - [x]* 5.2 Write property test for PDF paragraph preservation (Property 11)
    - **Property 11: PDF extraction preserves paragraph breaks as newlines**
    - **Validates: Requirements 8.1, 8.3**
    - Test class: `PdfExtractionPropertyTest`.
    - Programmatically generate PDFs with 2–10 random paragraphs using PDFBox; assert extracted text contains `\n` between paragraphs.

  - [x]* 5.3 Write unit tests for `PdfExtractorService`
    - Test extraction from a known fixture PDF; verify body is non-empty.
    - Test corrupt/image-only PDF returns `""`.
    - Test non-PDF MIME type throws `ValidationException`.
    - _Requirements: 3.6, 8.1, 8.2_

- [x] 6. Core backend services
  - [x] 6.1 Implement `ArticleService`
    - Create `com.blog.service.ArticleService` `@Service`.
    - `Page<ArticleResponse> listArticles(Pageable pageable)` — delegates to `ArticleRepository.findAllByOrderByPublishedAtDesc`.
    - `ArticleResponse getArticle(Long id)` — throws `ResourceNotFoundException` if absent.
    - `List<ArticleResponse> searchArticles(String q)` — throws `ValidationException` if `q` is blank; delegates to repository keyword search.
    - `ArticleResponse createArticle(ArticleRequest req, String source)` — validates title non-blank, persists, returns `ArticleResponse`.
    - _Requirements: 1.1, 1.4, 2.1, 3.1, 3.3, 5.2, 5.3, 5.5, 9.3_

  - [x]* 6.2 Write property test for article list ordering (Property 1)
    - **Property 1: Article list is always ordered by publication date descending**
    - **Validates: Requirements 1.1**
    - Test class: `ArticleListOrderPropertyTest`.
    - Insert N (1–50) articles with random `publishedAt`; assert each element's timestamp ≥ next element's.

  - [x]* 6.3 Write property test for required fields in response (Property 2)
    - **Property 2: Article list response always includes required fields**
    - **Validates: Requirements 1.2, 2.1**
    - Test class: `ArticleResponseFieldsPropertyTest`.
    - Generate articles with random field values; assert `id`, `title`, `author`, `publishedAt`, `likesCount` are all non-null in every `ArticleResponse`.

  - [x]* 6.4 Write property test for pagination page size (Property 3)
    - **Property 3: Pagination page size is never exceeded**
    - **Validates: Requirements 1.4**
    - Test class: `PaginationPropertyTest`.
    - Insert 0–100 random articles; for any valid page request assert response size ≤ 10.

  - [x]* 6.5 Write unit tests for `ArticleService`
    - Test `listArticles` delegates to repository and maps to `ArticleResponse`.
    - Test `getArticle` throws `ResourceNotFoundException` for unknown id.
    - Test `searchArticles` throws `ValidationException` for blank query.
    - Test `createArticle` throws `ValidationException` when title is blank.
    - _Requirements: 3.7, 5.5, 9.3_

  - [x] 6.6 Implement `LikeService`
    - Create `com.blog.service.LikeService` `@Service`.
    - `Map<String,Integer> like(Long id)` — loads article, increments `likesCount`, saves, returns `{"likesCount": N}`.
    - `Map<String,Integer> unlike(Long id)` — loads article; if `likesCount == 0` throws `ValidationException`; else decrements, saves, returns updated count.
    - Throws `ResourceNotFoundException` for unknown article id.
    - _Requirements: 6.2, 6.3, 6.5_

  - [x]* 6.7 Write property test for like increment (Property 8)
    - **Property 8: Like increments the count by exactly 1**
    - **Validates: Requirements 6.2**
    - Test class: `LikeIncrementPropertyTest`.
    - For articles with random initial `likesCount` (0–1000), assert count after like = initial + 1.

  - [x]* 6.8 Write property test for unlike decrement and floor (Property 9)
    - **Property 9: Unlike decrements the count by exactly 1, never below 0**
    - **Validates: Requirements 6.3, 6.5**
    - Test class: `LikeDecrementPropertyTest`.
    - For `likesCount > 0` assert count after unlike = N − 1; for `likesCount = 0` assert `ValidationException` / HTTP 400.

  - [x]* 6.9 Write unit tests for `LikeService`
    - Test like increments from arbitrary starting counts.
    - Test unlike decrements from N > 0.
    - Test unlike at 0 throws `ValidationException`.
    - _Requirements: 6.2, 6.3, 6.5_

  - [x] 6.10 Implement `GuestPostService`
    - Create `com.blog.service.GuestPostService` `@Service`.
    - `GuestPost submit(GuestPostRequest req, MultipartFile document)` — validate title/body non-blank; validate document MIME/size if present; persist with `status="PENDING"`; return saved entity.
    - `@Transactional ArticleResponse approve(Long id)` — load `GuestPost` by id (throw `ResourceNotFoundException` if absent); set `status="APPROVED"`; create `Article` from guest post fields; save both; return `ArticleResponse`.
    - _Requirements: 4.3, 4.5, 4.6, 4.7, 4.8_

  - [x]* 6.11 Write property test for guest post PENDING status (Property 5)
    - **Property 5: Guest post submission always results in PENDING status**
    - **Validates: Requirements 4.3**
    - Test class: `GuestPostSubmitPropertyTest`.
    - Generate random non-empty titles and bodies; assert persisted record has `status = "PENDING"` and response is HTTP 201.

  - [x]* 6.12 Write property test for guest post approval atomicity (Property 6)
    - **Property 6: Guest post approval is atomic**
    - **Validates: Requirements 4.8**
    - Test class: `GuestPostApprovalPropertyTest`.
    - Verify successful approve produces both `status = "APPROVED"` and a new article row.
    - Verify a forced failure during article creation rolls back the guest post status change.

  - [x]* 6.13 Write unit tests for `GuestPostService`
    - Test `submit` rejects blank title.
    - Test `submit` rejects document > 10 MB.
    - Test `submit` rejects unsupported MIME type.
    - Test `approve` throws `ResourceNotFoundException` for unknown id.
    - Test `approve` rollback on article-save failure.
    - _Requirements: 4.3, 4.5, 4.6, 4.8_

- [x] 7. Checkpoint — backend services
  - Ensure all backend unit and property tests pass.
  - Verify H2 dev profile starts without errors: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`.
  - Ask the user if any questions arise.

- [x] 8. REST controllers
  - [x] 8.1 Implement `ArticleController`
    - Create `com.blog.controller.ArticleController` `@RestController @RequestMapping("/api/articles")`.
    - `GET /api/articles` → `ArticleService.listArticles(Pageable)`; default page size 10.
    - `GET /api/articles/{id}` → `ArticleService.getArticle(id)`.
    - `GET /api/articles/search?q=…` → `ArticleService.searchArticles(q)`; return 400 if `q` absent/empty.
    - _Requirements: 1.1, 1.4, 2.1, 5.2, 5.3, 5.5_

  - [x]* 8.2 Write property test for search correctness (Property 7)
    - **Property 7: Search returns exactly the matching articles**
    - **Validates: Requirements 5.3**
    - Test class: `ArticleSearchPropertyTest`.
    - Insert random article sets; for random query strings assert result set = exact case-insensitive title/body matches (no false positives, no omissions).

  - [x] 8.3 Implement `LikeController`
    - Create `com.blog.controller.LikeController` `@RestController`.
    - `POST /api/articles/{id}/like` → `LikeService.like(id)` → 200 `{"likesCount": N}`.
    - `DELETE /api/articles/{id}/like` → `LikeService.unlike(id)` → 200 `{"likesCount": N}` or 400 if count already 0.
    - _Requirements: 6.2, 6.3, 6.5_

  - [x] 8.4 Implement `GuestPostController`
    - Create `com.blog.controller.GuestPostController` `@RestController @RequestMapping("/api/guest-posts")`.
    - `POST /api/guest-posts` — accept `@RequestPart GuestPostRequest` and optional `@RequestPart MultipartFile document`; delegate to `GuestPostService.submit`; return HTTP 201.
    - _Requirements: 4.1, 4.3, 4.5, 4.6_

  - [x] 8.5 Implement `AdminController`
    - Create `com.blog.controller.AdminController` `@RestController @RequestMapping("/api/admin")`.
    - `POST /api/admin/articles` — accept `@RequestBody ArticleRequest`; delegate to `ArticleService.createArticle`; return 201.
    - `POST /api/admin/articles/pdf` — accept `@RequestPart MultipartFile file`, optional `title`, optional `category`; call `PdfExtractorService.extract(file)` then `ArticleService.createArticle`; return 201.
    - `POST /api/admin/guest-posts/{id}/approve` — delegate to `GuestPostService.approve(id)`; return 200.
    - _Requirements: 3.1, 3.3, 3.4, 3.5, 3.6, 3.7, 4.7, 4.8_

- [x] 9. Backend integration tests
  - [x]* 9.1 Write Spring Boot integration tests with TestContainers
    - Spin up PostgreSQL via TestContainers.
    - Test complete request chains: admin article creation, PDF upload end-to-end, guest post approval transaction, search returning correct results.
    - _Requirements: 4.8, 5.3, 3.5_

- [x] 10. Checkpoint — full backend
  - Ensure all backend tests (unit, property, integration) pass.
  - Verify the API responds correctly on `localhost:8080` with the dev profile.
  - Ask the user if any questions arise.

- [x] 11. Frontend project scaffold
  - [x] 11.1 Create React app structure and package.json
    - Create `frontend/` directory using Create React App conventions.
    - Write `frontend/package.json` with dependencies: `react`, `react-dom`, `react-router-dom`, `axios`; devDependencies: `@testing-library/react`, `@testing-library/jest-dom`, `vitest` (or CRA default Jest).
    - Create directory structure: `src/api/`, `src/components/`, `src/pages/`, `src/utils/`.
    - Create `frontend/.env.example` with `REACT_APP_API_URL=http://localhost:8080`.
    - _Requirements: 10.1, 10.5_

- [x] 12. Frontend utilities and API client
  - [x] 12.1 Implement `apiClient.js`
    - Create `src/api/apiClient.js` — Axios instance with `baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080'`.
    - Export the instance as default.
    - _Requirements: 10.5_

  - [x] 12.2 Implement `localStorage.js` utility
    - Create `src/utils/localStorage.js`.
    - Export `isLiked(articleId): boolean` — reads a JSON set from `localStorage['likedArticles']`.
    - Export `setLiked(articleId, liked: boolean)` — adds or removes the id from the set and writes back.
    - _Requirements: 6.4_

- [x] 13. Shared UI components
  - [x] 13.1 Implement `Navbar` component
    - Create `src/components/Navbar.jsx`.
    - Render site title/logo linking to `/`.
    - Include navigation links: Home (`/`), Submit Post (`/submit`), About (`/about`).
    - Include a search `<input>` that, on form submit, navigates to `/?q={query}`.
    - _Requirements: 4.1, 5.1, 7.1_

  - [x] 13.2 Implement `ArticleCard` component
    - Create `src/components/ArticleCard.jsx`.
    - Props: `{ id, title, author, publishedAt, category }`.
    - Render as a clickable card linking to `/articles/:id`.
    - Display title, author (fall back to "Admin"), formatted date, category badge.
    - _Requirements: 1.2_

  - [x] 13.3 Implement `LikeButton` component
    - Create `src/components/LikeButton.jsx`.
    - Props: `{ articleId, initialLikes }`.
    - Read initial liked state from `localStorage.js`; show like count.
    - On click: if not liked → POST like, update count, call `setLiked(id, true)`; if liked → DELETE like, update count, call `setLiked(id, false)`.
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

  - [x] 13.4 Implement `Pagination` component
    - Create `src/components/Pagination.jsx`.
    - Props: `{ currentPage, totalPages, onPageChange }`.
    - Render Prev / Next buttons; disable at boundaries.
    - _Requirements: 1.4_

  - [x] 13.5 Implement `ErrorMessage` component
    - Create `src/components/ErrorMessage.jsx`.
    - Props: `{ message }`.
    - Render a styled error banner; render nothing if `message` is falsy.
    - _Requirements: 9.1, 9.2_

- [x] 14. Frontend pages
  - [x] 14.1 Implement `HomePage`
    - Create `src/pages/HomePage.jsx`.
    - On mount: read `?q` from URL; if present call `GET /api/articles/search?q=…`, else call `GET /api/articles?page=N`.
    - Render a list of `<ArticleCard>` items.
    - Render "No articles yet" (no search) or "No results found" (search) when list is empty.
    - Render `<Pagination>` for non-search mode.
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 5.1, 5.2, 5.4_

  - [x] 14.2 Implement `ArticleDetailPage`
    - Create `src/pages/ArticleDetailPage.jsx`.
    - On mount: call `GET /api/articles/:id`; show `<ErrorMessage>` on 404.
    - Display title, author, date, category, full body content.
    - If `documentUrl` is present, render a download link.
    - Render `<LikeButton articleId={id} initialLikes={likesCount} />`.
    - _Requirements: 2.1, 2.2, 2.3, 6.1_

  - [x] 14.3 Implement `SubmitPostPage`
    - Create `src/pages/SubmitPostPage.jsx`.
    - Render a form with: required `title` input, required `body` textarea, optional `authorName` input, optional `category` input, optional `document` file input.
    - Validate title and body non-empty before submit; show inline errors.
    - On valid submit: POST multipart to `/api/guest-posts`; show success message or `<ErrorMessage>`.
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

  - [x] 14.4 Implement `AboutPage`
    - Create `src/pages/AboutPage.jsx`.
    - Render static content: site title, purpose description, contact or call-to-action for guest post submissions.
    - No API calls.
    - _Requirements: 7.1, 7.2, 7.3_

- [x] 15. App wiring and global styles
  - [x] 15.1 Implement `App.jsx` with React Router routes
    - Create `src/App.jsx`.
    - Wrap content in `<BrowserRouter>`.
    - Render `<Navbar />` above the router outlet.
    - Define routes: `/` → `<HomePage>`, `/articles/:id` → `<ArticleDetailPage>`, `/submit` → `<SubmitPostPage>`, `/about` → `<AboutPage>`.
    - _Requirements: 1.5, 4.1, 7.1_

  - [x] 15.2 Write global CSS styling
    - Create `src/index.css` (or `src/App.css`).
    - Define baseline styles: reset, font-family, container max-width, card styles, button styles, error banner styles, responsive layout.
    - Import global CSS in `src/index.js`.
    - _Requirements: 10.1_

- [x] 16. Frontend tests
  - [x]* 16.1 Write unit/component tests
    - `HomePage` — renders article cards; shows "No articles yet" when empty; search input triggers correct API call.
    - `ArticleDetailPage` — displays required fields; like/unlike updates localStorage.
    - `SubmitPostPage` — form validation prevents submission with empty title/body.
    - `AboutPage` — renders without triggering any API calls.
    - _Requirements: 1.2, 1.3, 2.1, 4.4, 7.3_

- [x] 17. Deployment configuration
  - [x] 17.1 Create Netlify `_redirects` file
    - Create `frontend/public/_redirects` with content: `/*  /index.html  200`.
    - Ensures React Router deep links resolve correctly on Netlify.
    - _Requirements: 10.1_

  - [x] 17.2 Write `README.md`
    - Create root `README.md` covering: project overview, local development setup (backend dev profile, frontend `.env`), running tests, building for production, deploying to Railway/Render (JAR + Dockerfile), deploying frontend to Netlify, environment variable reference for both tiers.
    - _Requirements: 10.1, 10.2_

- [x] 18. Final checkpoint — full stack
  - Ensure all backend and frontend tests pass.
  - Verify frontend builds without errors: `npm run build`.
  - Ask the user if any questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP.
- Each task references specific requirements for traceability.
- Checkpoints (tasks 7, 10, 18) ensure incremental validation at logical boundaries.
- Property tests use jqwik (`@Property`, `@ForAll`) with a minimum of 100 iterations each.
- Unit tests use JUnit 5 + Mockito; integration tests use `@SpringBootTest` + TestContainers.
- Frontend tests use React Testing Library + Vitest (or CRA's default Jest).
- The dev Spring profile (`./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`) uses H2 — no external DB required locally.

---

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["2.1", "2.2"] },
    { "id": 2, "tasks": ["2.3", "2.4", "3.1", "3.2"] },
    { "id": 3, "tasks": ["3.3", "4.1", "4.2", "5.1"] },
    { "id": 4, "tasks": ["3.4", "4.3", "5.2", "5.3", "6.1", "6.6", "6.10"] },
    { "id": 5, "tasks": ["6.2", "6.3", "6.4", "6.5", "6.7", "6.8", "6.9", "6.11", "6.12", "6.13"] },
    { "id": 6, "tasks": ["8.1", "8.3", "8.4", "8.5"] },
    { "id": 7, "tasks": ["8.2", "9.1"] },
    { "id": 8, "tasks": ["11.1"] },
    { "id": 9, "tasks": ["12.1", "12.2"] },
    { "id": 10, "tasks": ["13.1", "13.2", "13.3", "13.4", "13.5"] },
    { "id": 11, "tasks": ["14.1", "14.2", "14.3", "14.4"] },
    { "id": 12, "tasks": ["15.1", "15.2"] },
    { "id": 13, "tasks": ["16.1", "17.1", "17.2"] }
  ]
}
```

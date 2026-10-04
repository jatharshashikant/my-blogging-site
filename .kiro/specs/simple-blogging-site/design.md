# Design Document — simple-blogging-site

## Overview

The simple blogging site is a two-tier web application: a React single-page application (SPA) served
from Netlify communicates with a Spring Boot REST API deployed on Railway or Render, backed by a
free-tier PostgreSQL database (Supabase or Railway PostgreSQL). Local development uses an H2
in-memory database activated by a Spring `dev` profile.

Key design goals:

- **Simplicity over complexity** — no authentication infrastructure (admin access is secured by a
  shared secret header `X-Admin-Key`); no WebSockets or server-sent events.
- **Separation of concerns** — frontend pages/components, backend controllers/services/repositories
  each live in their own files/packages.
- **Free-tier deployment** — the entire stack fits within the free tiers of Netlify, Railway/Render,
  and Supabase/Railway PostgreSQL.

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                  Browser (Netlify SPA)               │
│  React Router  │  Pages  │  Components  │  API util  │
└──────────────────────┬──────────────────────────────┘
                       │  HTTPS REST (JSON)
                       ▼
┌─────────────────────────────────────────────────────┐
│          Spring Boot API  (Railway / Render)         │
│  Controllers → Services → Repositories              │
│  PDF extraction: Apache PDFBox                      │
│  Multipart file handling: Spring MultipartFile      │
└──────────────────────┬──────────────────────────────┘
                       │  JDBC (HikariCP)
                       ▼
┌─────────────────────────────────────────────────────┐
│   PostgreSQL (Supabase / Railway) — prod            │
│   H2 in-memory — local dev (profile: dev)           │
└─────────────────────────────────────────────────────┘
```

### Cross-cutting concerns

| Concern | Approach |
|---|---|
| CORS | Spring `CorsConfigurationSource` bean; allowed origins read from `ALLOWED_ORIGINS` env var |
| Admin auth | `AdminAuthFilter` checks `X-Admin-Key` header against `ADMIN_KEY` env var; returns 403 on mismatch |
| Error responses | `GlobalExceptionHandler` (`@RestControllerAdvice`) produces `{"error":"…","message":"…"}` for all 4xx/5xx |
| File storage | Uploaded files saved to a local `uploads/` directory on the server; path stored in DB; for production a volume mount or object storage can replace this with a single line change |
| Pagination | Spring Data `Pageable`; default page size 10 |
| Transactions | `@Transactional` on the `GuestPostService.approve()` method |

---

## Components and Interfaces

### Frontend — React SPA

**Project root:** `frontend/`

```
frontend/
├── public/
├── src/
│   ├── api/
│   │   └── apiClient.js          # Axios instance; reads REACT_APP_API_URL
│   ├── components/
│   │   ├── Navbar.jsx             # Navigation bar with search input
│   │   ├── ArticleCard.jsx        # Summary card used in list view
│   │   ├── LikeButton.jsx         # Like/unlike toggle; reads/writes localStorage
│   │   ├── Pagination.jsx         # Prev/next page controls
│   │   └── ErrorMessage.jsx       # Generic error display
│   ├── pages/
│   │   ├── HomePage.jsx           # Article list + search results
│   │   ├── ArticleDetailPage.jsx  # Full article view + like button
│   │   ├── SubmitPostPage.jsx     # Guest post submission form
│   │   └── AboutPage.jsx          # Static about page
│   ├── utils/
│   │   └── localStorage.js        # get/set liked article IDs
│   ├── App.jsx                    # React Router routes
│   └── index.js
├── .env.example                   # REACT_APP_API_URL=http://localhost:8080
└── package.json
```

**Page ↔ API mapping:**

| Page | API calls |
|---|---|
| `HomePage` | `GET /api/articles?page=N` , `GET /api/articles/search?q=…` |
| `ArticleDetailPage` | `GET /api/articles/{id}` , `POST /api/articles/{id}/like` , `DELETE /api/articles/{id}/like` |
| `SubmitPostPage` | `POST /api/guest-posts` (multipart) |
| `AboutPage` | none |

---

### Backend — Spring Boot

**Project root:** `backend/`

```
backend/
├── src/main/java/com/blog/
│   ├── BlogApplication.java
│   ├── config/
│   │   ├── CorsConfig.java          # CorsConfigurationSource bean
│   │   └── SecurityConfig.java      # Permit-all (no Spring Security login)
│   ├── filter/
│   │   └── AdminAuthFilter.java     # OncePerRequestFilter for /api/admin/**
│   ├── controller/
│   │   ├── ArticleController.java   # GET /api/articles, GET /api/articles/{id}, search
│   │   ├── LikeController.java      # POST/DELETE /api/articles/{id}/like
│   │   ├── GuestPostController.java # POST /api/guest-posts
│   │   └── AdminController.java     # POST /api/admin/articles, /pdf, /guest-posts/{id}/approve
│   ├── service/
│   │   ├── ArticleService.java      # List, detail, search logic
│   │   ├── LikeService.java         # Increment / decrement logic
│   │   ├── GuestPostService.java    # Submit + approve (transactional)
│   │   └── PdfExtractorService.java # Apache PDFBox extraction
│   ├── repository/
│   │   ├── ArticleRepository.java   # Spring Data JPA
│   │   └── GuestPostRepository.java
│   ├── model/
│   │   ├── Article.java             # JPA entity
│   │   └── GuestPost.java           # JPA entity
│   ├── dto/
│   │   ├── ArticleRequest.java      # Admin article creation payload
│   │   ├── ArticleResponse.java     # Public article view
│   │   ├── GuestPostRequest.java    # Guest submission payload
│   │   └── ErrorResponse.java       # { error, message }
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       ├── ResourceNotFoundException.java
│       └── ValidationException.java
├── src/main/resources/
│   ├── application.properties       # Common config
│   ├── application-dev.properties   # H2 datasource
│   └── application-prod.properties  # PostgreSQL datasource (env vars)
└── pom.xml
```

**Controller → Service → Repository chain:**

```
ArticleController  →  ArticleService   →  ArticleRepository
LikeController     →  LikeService      →  ArticleRepository
GuestPostController→  GuestPostService →  GuestPostRepository, ArticleRepository
AdminController    →  ArticleService, GuestPostService, PdfExtractorService
```

---

## Data Models

### PostgreSQL schema

```sql
CREATE TABLE articles (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(500)  NOT NULL,
    body             TEXT          NOT NULL DEFAULT '',
    author           VARCHAR(255)  NOT NULL DEFAULT 'Admin',
    category         VARCHAR(255),
    likes_count      INTEGER       NOT NULL DEFAULT 0,
    source           VARCHAR(20)   NOT NULL DEFAULT 'ADMIN',  -- 'ADMIN' | 'GUEST'
    document_url     VARCHAR(1000),                           -- optional download link
    published_at     TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE guest_posts (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(500)  NOT NULL,
    body             TEXT          NOT NULL,
    author_name      VARCHAR(255),
    category         VARCHAR(255),
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING', -- 'PENDING' | 'APPROVED'
    document_url     VARCHAR(1000),
    submitted_at     TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Index for case-insensitive full-text-style keyword search
CREATE INDEX idx_articles_title_body ON articles USING gin(
    to_tsvector('english', title || ' ' || body)
);
```

### JPA Entity — Article

```java
@Entity @Table(name = "articles")
public class Article {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String body;
    private String author;
    private String category;
    private int likesCount;
    private String source;       // "ADMIN" | "GUEST"
    private String documentUrl;
    private LocalDateTime publishedAt;
}
```

### JPA Entity — GuestPost

```java
@Entity @Table(name = "guest_posts")
public class GuestPost {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String body;
    private String authorName;
    private String category;
    private String status;       // "PENDING" | "APPROVED"
    private String documentUrl;
    private LocalDateTime submittedAt;
}
```

### DTO — ErrorResponse

```java
public record ErrorResponse(String error, String message) {}
```

---

## API Endpoint Reference

All endpoints are prefixed with the base URL configured via `REACT_APP_API_URL`.

### Public endpoints

| Method | Path | Description | Response |
|---|---|---|---|
| `GET` | `/api/articles` | Paginated list (desc by date) | `200 Page<ArticleResponse>` |
| `GET` | `/api/articles/{id}` | Article detail | `200 ArticleResponse` / `404` |
| `GET` | `/api/articles/search?q={query}` | Keyword search | `200 List<ArticleResponse>` / `400` if q empty |
| `POST` | `/api/articles/{id}/like` | Increment like count | `200 {"likesCount": N}` |
| `DELETE` | `/api/articles/{id}/like` | Decrement like count | `200 {"likesCount": N}` / `400` if already 0 |
| `POST` | `/api/guest-posts` | Submit guest post (multipart) | `201 GuestPostResponse` / `400` |

### Admin endpoints (require `X-Admin-Key` header)

| Method | Path | Description | Response |
|---|---|---|---|
| `POST` | `/api/admin/articles` | Create article (JSON body) | `201 ArticleResponse` / `400` / `403` |
| `POST` | `/api/admin/articles/pdf` | Create article from PDF (multipart) | `201 ArticleResponse` / `400` / `403` |
| `POST` | `/api/admin/guest-posts/{id}/approve` | Approve guest post → Article | `200 ArticleResponse` / `403` / `404` / `500` |

### Query parameters

- `/api/articles?page=0&size=10` — zero-based page index, default size 10
- `/api/articles/search?q=keyword` — case-insensitive substring match on title + body

### Request/Response shapes

**POST /api/admin/articles** (JSON)
```json
{ "title": "string (required)", "body": "string (required)", "category": "string (optional)" }
```

**POST /api/admin/articles/pdf** (multipart/form-data)
- `file`: PDF binary (required)
- `title`: string (optional — defaults to filename without extension)
- `category`: string (optional)

**POST /api/guest-posts** (multipart/form-data)
- `title`: string (required)
- `body`: string (required)
- `authorName`: string (optional)
- `category`: string (optional)
- `document`: file (optional; PDF/DOC/DOCX, max 10 MB)

**ArticleResponse**
```json
{
  "id": 1,
  "title": "string",
  "body": "string",
  "author": "Admin",
  "category": "string",
  "likesCount": 0,
  "documentUrl": "string | null",
  "publishedAt": "2024-01-01T12:00:00"
}
```

**ErrorResponse**
```json
{ "error": "VALIDATION_ERROR", "message": "Title is required" }
```

---

## PDF Extraction Approach

**Library:** Apache PDFBox (`org.apache.pdfbox:pdfbox:3.x`)

**`PdfExtractorService`** logic:

```java
public String extract(MultipartFile file) throws IOException {
    try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        stripper.setLineSeparator("\n");
        stripper.setParagraphStart("\n");
        return stripper.getText(doc).strip();
    }
}
```

- `setSortByPosition(true)` ensures paragraphs are read in visual order.
- `setParagraphStart("\n")` preserves paragraph breaks as `\n` in the extracted string.
- If the result is blank (e.g., scanned image PDF with no selectable text), the body is stored as an
  empty string per requirement 3.5.
- MIME type validation uses `file.getContentType()` checked against `application/pdf` before
  invoking PDFBox; a non-PDF upload returns HTTP 400.

---

## CORS Configuration

```java
@Configuration
public class CorsConfig {
    @Value("${allowed.origins}")
    private String[] allowedOrigins;   // e.g., https://myblog.netlify.app

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins));
        config.setAllowedMethods(List.of("GET","POST","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-Admin-Key"));
        CorsRegistry registry = new UrlBasedCorsConfigurationSource();
        ((UrlBasedCorsConfigurationSource) registry).registerCorsConfiguration("/**", config);
        return (CorsConfigurationSource) registry;
    }
}
```

`allowed.origins` is populated from the `ALLOWED_ORIGINS` environment variable
(comma-separated list), e.g., `https://myblog.netlify.app,http://localhost:3000`.

---

## Environment Variable Strategy

### Frontend (build-time, `.env`)

| Variable | Purpose | Example |
|---|---|---|
| `REACT_APP_API_URL` | Backend base URL | `https://api.myblog.railway.app` |

Netlify reads `.env` variables set in the Netlify dashboard at build time. The `apiClient.js`
module reads `process.env.REACT_APP_API_URL` and falls back to `http://localhost:8080` for local
development.

```js
// src/api/apiClient.js
import axios from 'axios';
const apiClient = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080',
});
export default apiClient;
```

### Backend (runtime, environment variables)

| Variable | Purpose | Example |
|---|---|---|
| `ADMIN_KEY` | Shared secret for admin operations | `s3cr3t-k3y` |
| `ALLOWED_ORIGINS` | Comma-separated CORS origins | `https://myblog.netlify.app` |
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://…/blog` |
| `DB_USERNAME` | Database user | `postgres` |
| `DB_PASSWORD` | Database password | `…` |
| `UPLOAD_DIR` | Directory for uploaded files | `/app/uploads` |

```properties
# application-prod.properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
allowed.origins=${ALLOWED_ORIGINS}
admin.key=${ADMIN_KEY}
upload.dir=${UPLOAD_DIR:/app/uploads}
```

```properties
# application-dev.properties
spring.datasource.url=jdbc:h2:mem:blogdb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
allowed.origins=http://localhost:3000
admin.key=dev-admin-key
upload.dir=uploads
```

Activate the dev profile locally: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`

---

## Deployment Notes

### Frontend — Netlify

1. Build command: `npm run build`
2. Publish directory: `build/`
3. Set `REACT_APP_API_URL` in Netlify dashboard → Site settings → Environment variables.
4. Add `public/_redirects`:
   ```
   /*  /index.html  200
   ```
   This ensures React Router deep links resolve correctly.

### Backend — Railway / Render

**Option A — JAR deployment (Railway)**
1. Add a `Procfile`: `web: java -jar target/blog-0.0.1-SNAPSHOT.jar`
2. Railway auto-detects Maven and runs `mvn package`.
3. Set all environment variables in the Railway dashboard.
4. For uploads persistence: attach a Railway volume at `/app/uploads`.

**Option B — Docker**

```dockerfile
# Dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/blog-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar","--spring.profiles.active=prod"]
```

Build and push:
```bash
mvn clean package -DskipTests
docker build -t my-blog-api .
docker push <registry>/my-blog-api
```

### Database — Supabase / Railway PostgreSQL

1. Create a free PostgreSQL instance on Supabase or Railway.
2. Copy the JDBC connection string into `DB_URL`.
3. `spring.jpa.hibernate.ddl-auto=update` in prod will apply schema changes on startup
   (acceptable for a small site; migrate to Flyway if the project grows).

---

## Error Handling

All exceptions are handled by `GlobalExceptionHandler`:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(404)
            .body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> badRequest(ValidationException ex) {
        return ResponseEntity.status(400)
            .body(new ErrorResponse("VALIDATION_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> serverError(Exception ex) {
        return ResponseEntity.status(500)
            .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
```

`AdminAuthFilter` returns a 403 `ErrorResponse` before the request reaches a controller if
`X-Admin-Key` is absent or wrong.

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a
system — essentially, a formal statement about what the system should do. Properties serve as the
bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Article list is always ordered by publication date descending

*For any* set of published articles retrieved from the list endpoint, each article in the response
must have a `publishedAt` timestamp that is greater than or equal to the timestamp of every
subsequent article in the list.

**Validates: Requirements 1.1**

---

### Property 2: Article list response always includes required fields

*For any* article returned in the list or detail response, the payload must include non-null values
for `id`, `title`, `author`, `publishedAt`, and `likesCount`.

**Validates: Requirements 1.2, 2.1**

---

### Property 3: Pagination page size is never exceeded

*For any* page request to `/api/articles`, the number of articles in the response body must be less
than or equal to 10.

**Validates: Requirements 1.4**

---

### Property 4: Admin endpoints reject any request without a valid key

*For any* request body or payload directed at a `/api/admin/**` endpoint, if the `X-Admin-Key`
header is absent, empty, or does not match the configured secret, the response status must be
exactly HTTP 403.

**Validates: Requirements 3.2**

---

### Property 5: Guest post submission always results in PENDING status

*For any* guest post submitted with a non-empty title and non-empty body, the persisted record
must have `status = "PENDING"` and the response must be HTTP 201.

**Validates: Requirements 4.3**

---

### Property 6: Guest post approval is atomic

*For any* PENDING guest post that is approved successfully, the resulting state must satisfy both:
(a) the guest post record has `status = "APPROVED"`, and (b) a corresponding article record exists
in the articles table. These two changes must occur within a single transaction — if either fails,
neither change should persist.

**Validates: Requirements 4.8**

---

### Property 7: Search returns exactly the matching articles (no false positives, no omissions)

*For any* non-empty search query and any set of articles, the search endpoint must return exactly
those articles whose `title` or `body` contains the query string (case-insensitive). No article
containing the query should be absent from the results; no article not containing the query should
appear in the results.

**Validates: Requirements 5.3**

---

### Property 8: Like increments the count by exactly 1

*For any* article with a current like count of N, after a successful POST to
`/api/articles/{id}/like`, the article's `likesCount` must equal N + 1.

**Validates: Requirements 6.2**

---

### Property 9: Unlike decrements the count by exactly 1, never below 0

*For any* article with a current like count of N > 0, after a successful DELETE to
`/api/articles/{id}/like`, the article's `likesCount` must equal N − 1. For any article with
`likesCount = 0`, a DELETE request must return HTTP 400.

**Validates: Requirements 6.3, 6.5**

---

### Property 10: All error responses conform to the standard schema

*For any* request that results in an HTTP 4xx or 5xx response, the response body must be a JSON
object containing exactly the fields `error` (a non-empty string error code) and `message` (a
non-empty human-readable string). No error response should omit either field or return a different
schema.

**Validates: Requirements 9.1, 9.2, 9.4**

---

### Property 11: PDF extraction preserves paragraph breaks as newlines

*For any* valid PDF document containing multiple paragraphs, the text extracted by
`PdfExtractorService` must contain at least one `\n` character separating distinct paragraphs from
the source document.

**Validates: Requirements 8.1, 8.3**

---

## Testing Strategy

### Unit tests (JUnit 5 + Mockito)

Unit tests verify specific examples, edge cases, and error conditions in isolation.

Focus areas:
- `PdfExtractorService` — extract from known PDFs, handle corrupt/image-only PDFs, verify
  `\n` paragraph separators.
- `LikeService` — like increments count; unlike decrements count; unlike at 0 throws
  `ValidationException`.
- `GuestPostService.approve()` — verify article is created and guest post marked APPROVED; verify
  rollback when article creation fails.
- `AdminAuthFilter` — missing key → 403; wrong key → 403; correct key → passes through.
- `GlobalExceptionHandler` — each exception type maps to the correct HTTP status and
  `ErrorResponse` shape.

### Property-based tests (jqwik)

Library: **jqwik** (`net.jqwik:jqwik`) — a property-based testing library for JUnit 5 on the JVM.
Each property test runs a minimum of **100 iterations** with randomly generated inputs.

Each test is annotated with a comment in the format:
`// Feature: simple-blogging-site, Property N: <property text>`

| Property | Test class | What varies |
|---|---|---|
| P1: Article list ordering | `ArticleListOrderPropertyTest` | Sets of articles with random `publishedAt` timestamps |
| P2: Required fields present | `ArticleResponseFieldsPropertyTest` | Articles with random field values |
| P3: Page size ≤ 10 | `PaginationPropertyTest` | Total article counts from 0 to 100 |
| P4: Admin auth rejects invalid keys | `AdminAuthPropertyTest` | Random key strings (none matching the secret) |
| P5: Guest post status = PENDING | `GuestPostSubmitPropertyTest` | Random valid titles and bodies |
| P6: Approval atomicity | `GuestPostApprovalPropertyTest` | Random guest post content; forced-failure variant |
| P7: Search correctness | `ArticleSearchPropertyTest` | Random article sets + random query strings |
| P8: Like increment | `LikeIncrementPropertyTest` | Articles with random initial like counts |
| P9: Unlike decrement + floor | `LikeDecrementPropertyTest` | Articles with random like counts ≥ 0 |
| P10: Error response schema | `ErrorResponseSchemaPropertyTest` | Various invalid requests |
| P11: PDF paragraph newlines | `PdfExtractionPropertyTest` | PDFs generated with varying paragraph counts |

### Integration tests (Spring Boot Test + TestContainers)

- Use `@SpringBootTest` with a TestContainers PostgreSQL container for full-stack endpoint tests.
- Cover the complete request path: HTTP → Controller → Service → Repository → DB.
- Focus on: guest post approval transaction, admin PDF upload end-to-end, search results from real
  DB queries.

### Frontend tests (React Testing Library + Vitest)

- `HomePage` — renders article cards; displays "No articles yet" when empty; search input triggers
  correct API call.
- `ArticleDetailPage` — displays all required fields; like/unlike button updates localStorage.
- `SubmitPostPage` — form validation prevents empty title/body submission.
- `AboutPage` — renders with no API calls triggered.

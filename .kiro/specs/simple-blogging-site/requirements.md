# Requirements Document

## Introduction

A simple, publicly accessible blogging site where an admin can publish articles (including PDF uploads rendered as articles), and guest users can submit their own posts with optional metadata. The site supports article search, likes/unlikes per article, and an About page. The frontend is built with React/JavaScript/HTML/CSS, the backend with Java/Spring Boot, and data is persisted in a free-tier database. The site is hosted on Netlify (frontend) and Railway or Render (backend).

---

## Glossary

- **System**: The blogging site as a whole, encompassing frontend and backend.
- **Frontend**: The React-based single-page application served from Netlify.
- **Backend**: The Spring Boot REST API deployed on Railway or Render.
- **Database**: The free-tier relational database (e.g., Supabase PostgreSQL or Railway PostgreSQL).
- **Admin**: The privileged operator who manages and publishes articles via a secret admin key (no login UI required).
- **Guest_User**: Any anonymous visitor who reads articles or submits a guest post.
- **Article**: A published piece of content visible to all visitors, created from either a direct text submission or a converted PDF.
- **Guest_Post**: A submission from a Guest_User that includes optional name, category, and an optional uploaded document, pending admin approval before publication.
- **PDF_Converter**: The backend component that extracts text content from an uploaded PDF file.
- **Search_Engine**: The backend component responsible for filtering articles by keyword.
- **Like_Service**: The backend component that tracks like/unlike counts per article.
- **About_Page**: A static frontend page describing the purpose of the site.

---

## Requirements

### Requirement 1: Public Article Listing

**User Story:** As a Guest_User, I want to browse a list of published articles, so that I can discover and read content on the site.

#### Acceptance Criteria

1. THE System SHALL display a paginated list of published Articles on the home page, ordered by publication date descending.
2. WHEN the home page loads, THE Frontend SHALL request the list of Articles from the Backend and render each Article's title, author name (or "Admin"), publication date, and category.
3. WHEN no Articles have been published, THE Frontend SHALL display a "No articles yet" message.
4. THE System SHALL display a maximum of 10 Articles per page.
5. WHEN at least one Article exists and a Guest_User selects an Article from the list, THE Frontend SHALL navigate to the Article detail page and display the full content.

---

### Requirement 2: Article Detail View

**User Story:** As a Guest_User, I want to read the full content of an article, so that I can consume the information shared.

#### Acceptance Criteria

1. WHEN a Guest_User navigates to an Article detail page, THE Frontend SHALL display the Article's title, author, publication date, category, and full body content.
2. WHEN an Article was created from a PDF upload, THE Frontend SHALL display the extracted text content of the PDF as the Article body.
3. WHEN an Article has an associated uploaded document from a Guest_Post, THE Frontend SHALL display a download link for that document.

---

### Requirement 3: Admin Article Publishing

**User Story:** As an Admin, I want to publish articles directly or via PDF upload, so that I can share my thoughts on the site.

#### Acceptance Criteria

1. THE Backend SHALL expose a POST `/api/admin/articles` endpoint that accepts a title, body text, and optional category.
2. WHEN a request to `/api/admin/articles` is received without a valid admin key in the `X-Admin-Key` request header, THE Backend SHALL return HTTP 403.
3. WHEN a valid admin key is present and all required fields are provided, THE Backend SHALL persist the Article to the Database and return HTTP 201 with the created Article payload.
4. THE Backend SHALL expose a POST `/api/admin/articles/pdf` endpoint that accepts a multipart PDF file upload along with an optional title and category.
5. WHEN a valid PDF file is uploaded to `/api/admin/articles/pdf`, THE PDF_Converter SHALL extract the text content from the PDF and THE Backend SHALL persist the resulting Article to the Database (using an empty string for body if extraction yields no text) and return HTTP 201.
6. IF the uploaded file is not a valid PDF (wrong MIME type or corrupt content), THEN THE Backend SHALL return HTTP 400 with a descriptive error message.
7. IF required fields (title) are missing from an admin article submission, THEN THE Backend SHALL return HTTP 400 with a descriptive error message.

---

### Requirement 4: Guest Post Submission

**User Story:** As a Guest_User, I want to submit my own thoughts as a guest post, so that I can contribute content to the site.

#### Acceptance Criteria

1. THE Frontend SHALL provide a "Submit a Post" form accessible from the main navigation.
2. THE Guest_Post submission form SHALL include a required title field, a required body text field, an optional author name field, an optional category field, and an optional document upload field.
3. WHEN a Guest_User submits the form with a valid title and body, THE Backend SHALL persist the Guest_Post with a status of `PENDING` and return HTTP 201.
4. WHEN a Guest_User submits the form without a title or body, THE Frontend SHALL display a validation error and SHALL NOT submit the form to the Backend.
5. WHERE a Guest_User chooses to upload a document, THE Backend SHALL accept files with MIME types `application/pdf`, `application/msword`, or `application/vnd.openxmlformats-officedocument.wordprocessingml.document`, with a maximum file size of 10 MB.
6. IF an uploaded document exceeds 10 MB or has an unsupported MIME type, THEN THE Backend SHALL return HTTP 400 with a descriptive error message.
7. THE Backend SHALL expose a POST `/api/admin/guest-posts/{id}/approve` endpoint so THE Admin can approve a PENDING Guest_Post, converting it into a published Article.
8. WHEN a PENDING Guest_Post is approved, THE Backend SHALL atomically set the Guest_Post status to `APPROVED` and create a corresponding Article record in the Database within a single database transaction, returning HTTP 200 on success, or HTTP 500 with a rollback if either operation fails.

---

### Requirement 5: Article Search

**User Story:** As a Guest_User, I want to search for articles by keyword, so that I can quickly find content relevant to my interests.

#### Acceptance Criteria

1. THE Frontend SHALL provide a search input visible on the home page and in the main navigation.
2. WHEN a Guest_User submits a search query of at least 1 character, THE Frontend SHALL send a GET request to `/api/articles/search?q={query}` and display the matching results.
3. WHEN the search query is submitted, THE Search_Engine SHALL return Articles whose title or body content contains the query string (case-insensitive).
4. WHEN no Articles match the query, THE Frontend SHALL display a "No results found" message.
5. IF the search query parameter is missing or empty, THEN THE Backend SHALL return HTTP 400 with a descriptive error message.

---

### Requirement 6: Article Likes

**User Story:** As a Guest_User, I want to like or unlike an article, so that I can express appreciation for content I enjoy.

#### Acceptance Criteria

1. THE Frontend SHALL display a like count and a like/unlike toggle button on each Article detail page.
2. WHEN a Guest_User clicks the like button on an Article, THE Frontend SHALL send a POST request to `/api/articles/{id}/like` and THE Like_Service SHALL increment the like count for that Article by 1.
3. WHEN a Guest_User clicks the unlike button on an Article they have already liked (tracked via browser `localStorage`), THE Frontend SHALL send a DELETE request to `/api/articles/{id}/like` and THE Like_Service SHALL decrement the like count for that Article by 1, with a minimum value of 0.
4. THE Frontend SHALL persist the liked state of each Article in browser `localStorage` so that the like state is preserved across page refreshes within the same browser session.
5. IF a DELETE request is received for an Article whose like count is already 0, THEN THE Backend SHALL return HTTP 400 with a descriptive error message, regardless of client state or retries.

---

### Requirement 7: About Page

**User Story:** As a Guest_User, I want to visit an About page, so that I can understand the purpose of the site.

#### Acceptance Criteria

1. THE Frontend SHALL include an About page accessible via the `/about` route and a link in the main navigation.
2. THE About_Page SHALL display a title, a written description of the site's purpose, and contact information or a call-to-action for guest post submissions.
3. THE About_Page SHALL be rendered as a static page with no Backend requests required.

---

### Requirement 8: PDF Parsing Round-Trip Integrity

**User Story:** As an Admin, I want the PDF extraction to faithfully represent the document content, so that published articles are accurate.

#### Acceptance Criteria

1. WHEN THE PDF_Converter extracts text from a valid PDF, THE extracted text SHALL contain all human-readable paragraphs present in the source PDF.
2. FOR ALL valid PDF documents uploaded via the admin endpoint, parsing the PDF and storing the extracted text SHALL produce an Article whose body content is non-empty.
3. THE PDF_Converter SHALL preserve paragraph breaks from the source PDF as newline characters in the extracted text.

---

### Requirement 9: API Error Handling

**User Story:** As a developer integrating with the Backend, I want consistent error responses, so that the Frontend can display meaningful messages to users.

#### Acceptance Criteria

1. WHEN THE Backend encounters a validation error, THE Backend SHALL return HTTP 400 with a JSON body containing an `error` field and a human-readable `message` field.
2. WHEN THE Backend encounters an internal server error, THE Backend SHALL return HTTP 500 with a JSON body containing an `error` field and a human-readable `message` field.
3. WHEN a requested Article does not exist, THE Backend SHALL return HTTP 404 with a descriptive error message.
4. THE Backend SHALL return all error responses in the same JSON schema: `{ "error": "<error_code>", "message": "<human-readable description>" }`.

---

### Requirement 10: Deployment and Hosting

**User Story:** As an Admin, I want the site hosted on free-tier services with a publicly accessible URL, so that any user can visit without cost to me.

#### Acceptance Criteria

1. THE Frontend SHALL be deployable to Netlify via a build output of static files generated by `npm run build`.
2. THE Backend SHALL be deployable to Railway or Render using a standard Java JAR artifact or a Docker container.
3. THE System SHALL use a free-tier PostgreSQL database (e.g., Supabase or Railway PostgreSQL) as its persistent store.
4. WHERE the application is run in a local development environment, THE System SHALL support using an H2 in-memory database via a Spring profile (`dev`) without requiring any external database setup.
5. THE Frontend SHALL read the Backend base URL from a build-time environment variable (`REACT_APP_API_URL`) so that the same build artifact can target different environments.

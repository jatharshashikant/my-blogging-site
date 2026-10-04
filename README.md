# Simple Blogging Site

A full-stack blogging application with a React frontend and Spring Boot backend. The site supports article publishing, guest post submissions, article search, and like/unlike functionality.

## 🎯 Features

- ✅ **Public Article Listing** — Browse articles paginated by publication date (10 per page)
- ✅ **Article Detail View** — Read full article content with metadata, likes, and document downloads
- ✅ **Admin Article Publishing** — Create articles directly or via PDF upload (extracts text content)
- ✅ **Guest Post Submission** — Submit articles with optional documents pending admin approval
- ✅ **Article Search** — Case-insensitive keyword search across titles and bodies
- ✅ **Like/Unlike System** — Track likes per article with localStorage persistence
- ✅ **About Page** — Static information page
- ✅ **PDF Text Extraction** — Converts PDF documents to article content
- ✅ **CORS Support** — Frontend and backend communicate securely across origins
- ✅ **Error Handling** — Consistent error response schema

## 📦 Project Structure

```
my_blogging_site/
├── backend/
│   ├── src/main/java/com/blog/
│   │   ├── BlogApplication.java        # Spring Boot entry point
│   │   ├── config/                     # CORS & security config
│   │   ├── controller/                 # REST endpoints
│   │   ├── service/                    # Business logic
│   │   ├── repository/                 # Data access layer
│   │   ├── model/                      # JPA entities
│   │   ├── dto/                        # Request/response objects
│   │   ├── exception/                  # Error handling
│   │   └── filter/                     # Security filters
│   ├── src/main/resources/
│   │   ├── application.properties      # Common config
│   │   ├── application-dev.properties  # Dev profile (H2)
│   │   └── application-prod.properties # Prod profile (PostgreSQL)
│   ├── src/test/java/                  # Unit, integration, property tests
│   ├── pom.xml                         # Maven dependencies
│   └── mvnw, mvnw.cmd                  # Maven wrapper scripts
│
├── frontend/
│   ├── src/
│   │   ├── api/
│   │   │   └── apiClient.js            # Axios HTTP client
│   │   ├── components/                 # Reusable UI components
│   │   ├── pages/                      # Page components
│   │   ├── utils/                      # Helper functions (localStorage)
│   │   ├── App.jsx                     # React Router setup
│   │   └── index.js                    # Entry point
│   ├── public/
│   │   └── _redirects                  # Netlify SPA routing config
│   ├── .env.example                    # Environment variable template
│   └── package.json                    # Dependencies
│
└── README.md                           # This file
```

## 🚀 Local Development

### Prerequisites

- **Java 21+** — for backend
- **Node.js 18+** — for frontend
- **Maven** — included as `mvnw` wrapper

### Backend Setup

1. **Navigate to backend:**
   ```bash
   cd backend
   ```

2. **Start dev server** (uses H2 in-memory database, no external DB needed):
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```
   
   The API will be available at `http://localhost:8080`

3. **H2 Console** (view database):
   - URL: `http://localhost:8080/h2-console`
   - JDBC URL: `jdbc:h2:mem:blogdb`
   - Default credentials: username `sa`, password (empty)

### Frontend Setup

1. **Install dependencies:**
   ```bash
   cd frontend
   npm install
   ```

2. **Create `.env` file** (copy from `.env.example`):
   ```bash
   cp .env.example .env
   ```

3. **Start dev server:**
   ```bash
   npm start
   ```
   
   The app will open at `http://localhost:3000`

4. **Admin Key** — For local development, use `dev-admin-key` as the admin key

## 🧪 Running Tests

### Backend Tests

Run all unit, property-based, and integration tests:
```bash
cd backend
./mvnw test
```

**Test Coverage:**
- **Unit Tests** — Specific examples and edge cases for services, filters, handlers
- **Property-Based Tests (jqwik)** — Randomized testing to verify universal properties (100+ iterations each):
  - Article list ordering (descending by publication date)
  - Required fields always present
  - Pagination size never exceeded
  - Admin auth rejection of invalid keys
  - Guest post PENDING status
  - Approval atomicity
  - Search correctness
  - Like/unlike increment/decrement
  - Error response schema compliance
  - PDF paragraph preservation
- **Integration Tests** — Full request/response cycles with test database (TestContainers)

**Test Results:** 40 tests, all passing

### Frontend Tests

Run component and unit tests:
```bash
cd frontend
npm test -- --coverage --watchAll=false
```

**Test Coverage:**
- Component rendering (ArticleCard, Pagination, LikeButton)
- User interactions (button clicks, form inputs)
- API integration and state management
- localStorage persistence
- Page behavior (navigation, error states)

**Test Results:** 64 tests, all passing

### Watch Mode (Auto-rerun on file change)

**Backend:**
```bash
cd backend
./mvnw test -Dtest=YourTestClass
```

**Frontend:**
```bash
cd frontend
npm test
```
(Press `a` to run all tests, `w` to watch mode)

## 🏗️ Building for Production

### Frontend Build

Create an optimized production bundle:
```bash
cd frontend
npm run build
```

Output is in `frontend/build/` directory. The build process:
- Minifies JavaScript and CSS
- Optimizes images
- Creates sourcemaps
- Builds to ~77 KB gzipped (production ready)

**Verify build works locally:**
```bash
npm install -g serve
serve -s build
# Opens http://localhost:3000
```

### Backend Build

Create a JAR artifact for deployment:
```bash
cd backend
./mvnw clean package -DskipTests
```

Output: `backend/target/blog-0.0.1-SNAPSHOT.jar`

**Test the JAR locally** (requires PostgreSQL or external database):
```bash
java -jar backend/target/blog-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  -DDB_URL=jdbc:postgresql://localhost:5432/blog \
  -DDB_USERNAME=postgres \
  -DDB_PASSWORD=your_password
```

## 🌐 Deploying to Production

### Frontend — Netlify

1. **Connect repository:**
   - Go to [Netlify](https://app.netlify.com) → New site from Git
   - Select your repository
   - Choose GitHub/GitLab provider

2. **Configure build:**
   - Build command: `cd frontend && npm run build`
   - Publish directory: `frontend/build`

3. **Set environment variables:**
   - In Netlify Dashboard → Site settings → Environment variables
   - Add: `REACT_APP_API_URL=https://your-backend-url.railway.app`

4. **Deploy:**
   - Push to main branch
   - Netlify auto-builds and deploys
   - Site URL: `https://your-site.netlify.app`

5. **Verify React Router:** The `_redirects` file ensures deep links work correctly

### Backend — Railway or Render

#### Option A: Render

1. **Create web service:**
   - Go to [Render](https://dashboard.render.com) → New → Web Service
   - Connect your repository

2. **Configure:**
   - Runtime: Java
   - Build command: `./mvnw clean package -DskipTests`
   - Start command: `java -jar target/blog-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod`

3. **Set environment variables** (Dashboard → Environment):
   - `DB_URL` — PostgreSQL connection string
   - `DB_USERNAME` — Database user
   - `DB_PASSWORD` — Database password
   - `ADMIN_KEY` — Secret admin key (e.g., `secure-key-12345`)
   - `ALLOWED_ORIGINS` — `https://your-frontend.netlify.app`

4. **Deploy:** Push to main → Render auto-deploys

#### Option B: Railway

1. **Create new project:**
   - Go to [Railway](https://railway.app) → New Project
   - Import from GitHub

2. **Configure:**
   - Railway auto-detects Maven and builds
   - Add `Procfile`: `web: java -jar target/blog-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod`

3. **Add PostgreSQL:**
   - Dashboard → Add service → PostgreSQL
   - Railway auto-creates `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`

4. **Set additional variables:**
   - `ADMIN_KEY`
   - `ALLOWED_ORIGINS`

5. **Deploy:** Push to main → Railway auto-builds and deploys

### Database — PostgreSQL (Free Tier)

#### Supabase (Recommended for simplicity)

1. **Create account** at [Supabase](https://supabase.com)

2. **Create project:**
   - Project name: `blogging-site`
   - Region: Choose closest to you
   - Password: Strong password

3. **Copy connection string:**
   - Settings → Database → Connection string (URI format)
   - Use as `DB_URL`

#### Railway PostgreSQL

If using Railway for backend, add PostgreSQL:
- Dashboard → Add service → PostgreSQL
- Railway auto-provides `DB_URL`, credentials

### Environment Variables Reference

#### Backend (Set on deployment platform)

| Variable | Purpose | Example |
|----------|---------|---------|
| `DB_URL` | PostgreSQL JDBC connection string | `jdbc:postgresql://db.supabase.co:5432/postgres` |
| `DB_USERNAME` | Database user | `postgres` |
| `DB_PASSWORD` | Database password | `your_secure_password` |
| `ADMIN_KEY` | Secret key for admin operations | `my-admin-secret-key-123` |
| `ALLOWED_ORIGINS` | CORS allowed origins (comma-separated) | `https://myblog.netlify.app` |
| `UPLOAD_DIR` | Directory for uploaded files (optional) | `/app/uploads` |
| `spring.profiles.active` | Active Spring profile | `prod` |

#### Frontend (Set in Netlify environment variables)

| Variable | Purpose | Example |
|----------|---------|---------|
| `REACT_APP_API_URL` | Backend API base URL | `https://api.myblog.railway.app` |

## 🏛️ Architecture Overview

### Layered Design

```
Request → Controller → Service → Repository → Database
Response ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ←
```

**Components:**

- **Controllers** (`ArticleController`, `AdminController`, `LikeController`, `GuestPostController`) — Handle HTTP routing, delegate to services
- **Services** (`ArticleService`, `LikeService`, `GuestPostService`, `PdfExtractorService`) — Encapsulate business logic, coordinate repositories
- **Repositories** (`ArticleRepository`, `GuestPostRepository`) — Data access via Spring Data JPA
- **Models** (`Article`, `GuestPost`) — JPA-mapped database entities
- **DTOs** (`ArticleRequest`, `ArticleResponse`, `ErrorResponse`) — Decouple API from internal structure

### Key Patterns

**DTO Pattern** — Request/response objects are independent from entities
- Protects internal database structure
- Enables API versioning
- Clearer contracts between frontend and backend

**Exception Handling** — Centralized via `@RestControllerAdvice`
- Consistent error response format: `{"error":"CODE", "message":"description"}`
- Maps specific exceptions to HTTP status codes
- No stack traces exposed to clients

**Configuration Management** — Three-tier approach
1. `application.properties` — Common config
2. `application-{profile}.properties` — Profile-specific (dev, prod)
3. Environment variables — Sensitive data, overrides

**CORS Configuration** — Explicit allowlist of origins
- Prevents unauthorized cross-origin requests
- Configurable via `ALLOWED_ORIGINS` environment variable

**Admin Authentication** — Header-based secret
- `X-Admin-Key` header checked by `AdminAuthFilter`
- Returns 403 if missing or mismatched
- No database lookup needed

**PDF Extraction** — Apache PDFBox with text stripper
- Extracts text content preserving paragraphs
- Stores extracted content as article body
- Handles corrupt/scanned PDFs gracefully

### Database Schema

**Articles Table:**
- `id` — Primary key (auto-increment)
- `title` — Article title
- `body` — Article content (or extracted PDF text)
- `author` — Author name (default: "Admin")
- `category` — Optional category tag
- `likes_count` — Like counter (default: 0)
- `source` — "ADMIN" or "GUEST"
- `document_url` — Optional download link to source document
- `published_at` — Publication timestamp (auto-set)

**Guest Posts Table:**
- `id` — Primary key
- `title` — Submission title
- `body` — Submission content
- `author_name` — Submitter name (optional)
- `category` — Optional category
- `status` — "PENDING" or "APPROVED"
- `document_url` — Optional uploaded document
- `submitted_at` — Submission timestamp

### Frontend Component Structure

**Pages:**
- `HomePage` — Article list with search and pagination
- `ArticleDetailPage` — Full article view with like button
- `SubmitPostPage` — Guest post form
- `AboutPage` — Static content

**Shared Components:**
- `Navbar` — Navigation with search input
- `ArticleCard` — Article summary card
- `LikeButton` — Like/unlike toggle with count
- `Pagination` — Page controls
- `ErrorMessage` — Error display banner

**State Management:**
- React hooks (`useState`, `useEffect`) for component state
- `localStorage` for likes persistence
- Axios for API client

**Styling:**
- CSS Modules for component isolation
- Global styles in `index.css`
- Responsive design (mobile-first)

## 🔐 Security

- **Admin Operations** — Protected by `X-Admin-Key` header
- **CORS** — Configurable origin allowlist
- **Error Responses** — No sensitive information leaked
- **File Uploads** — MIME type validation (PDF/DOC/DOCX), 10 MB size limit
- **Database** — Parameterized queries prevent SQL injection
- **Transactions** — `@Transactional` ensures data consistency on multi-step operations

## 📋 API Endpoints

### Public Endpoints

| Method | Path | Description | Response |
|--------|------|-------------|----------|
| `GET` | `/api/articles` | List articles (paginated, desc by date) | `Page<ArticleResponse>` |
| `GET` | `/api/articles/{id}` | Get article by id | `ArticleResponse` |
| `GET` | `/api/articles/search?q={query}` | Search articles by keyword | `List<ArticleResponse>` |
| `POST` | `/api/articles/{id}/like` | Increment like count | `{"likesCount": N}` |
| `DELETE` | `/api/articles/{id}/like` | Decrement like count | `{"likesCount": N}` |
| `POST` | `/api/guest-posts` | Submit guest post (multipart) | `GuestPostResponse` |

### Admin Endpoints (require `X-Admin-Key` header)

| Method | Path | Description | Response |
|--------|------|-------------|----------|
| `POST` | `/api/admin/articles` | Create article | `ArticleResponse` |
| `POST` | `/api/admin/articles/pdf` | Create article from PDF | `ArticleResponse` |
| `POST` | `/api/admin/guest-posts/{id}/approve` | Approve guest post → Article | `ArticleResponse` |

## 🛠️ Development Commands

### Backend

```bash
cd backend

# Dev server (H2 in-memory, no external DB)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run tests
./mvnw test

# Build production JAR
./mvnw clean package -DskipTests

# View H2 console
# http://localhost:8080/h2-console
```

### Frontend

```bash
cd frontend

# Install dependencies
npm install

# Start dev server with hot reload
npm start

# Run tests (watch mode)
npm test

# Run tests once with coverage
npm test -- --coverage --watchAll=false

# Build production bundle
npm run build

# Preview production build
npm run preview
```

## 📚 Technology Stack

### Backend
- **Framework** — Spring Boot 3.2
- **Language** — Java 21
- **Build Tool** — Maven
- **Database** — PostgreSQL (prod), H2 (dev)
- **Testing** — JUnit 5, Mockito, jqwik (property-based testing)
- **PDF Handling** — Apache PDFBox
- **ORM** — Spring Data JPA / Hibernate

### Frontend
- **Framework** — React 18
- **Build Tool** — Create React App
- **Routing** — React Router v6
- **HTTP Client** — Axios
- **Testing** — React Testing Library, Jest
- **Styling** — CSS (CSS Modules friendly)

## 📖 Documentation

See the specification for detailed requirements:
- `STEERING.md` — Architecture principles and best practices
- `.kiro/specs/simple-blogging-site/` — Complete specification

## ✅ Deployment Checklist

- [ ] Backend tests passing (`./mvnw test`)
- [ ] Frontend tests passing (`npm test -- --coverage --watchAll=false`)
- [ ] Frontend builds successfully (`npm run build`)
- [ ] Backend builds successfully (`./mvnw clean package -DskipTests`)
- [ ] PostgreSQL database created and accessible
- [ ] Environment variables set on deployment platform
- [ ] ALLOWED_ORIGINS includes frontend domain
- [ ] ADMIN_KEY is strong and secure
- [ ] Frontend deployed to Netlify
- [ ] Backend deployed to Railway/Render
- [ ] Both tiers can communicate (test with an API call)
- [ ] Articles display on frontend
- [ ] Search functionality works
- [ ] Like/unlike functionality works
- [ ] Admin endpoints accessible with correct key

## 🐛 Troubleshooting

### Backend won't start

**Problem:** `Connection refused` when connecting to database
**Solution:** Ensure PostgreSQL is running. In dev mode, H2 requires no setup: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`

### CORS errors in browser console

**Problem:** `Cross-Origin Request Blocked` error
**Solution:** Ensure `ALLOWED_ORIGINS` on backend includes your frontend URL. Format: `https://your-frontend.netlify.app`

### Frontend can't connect to backend

**Problem:** API calls fail with network errors
**Solution:** Check `REACT_APP_API_URL` environment variable. Verify backend is running and accessible from frontend. Check browser DevTools → Network tab for actual URL being called.

### Admin key doesn't work

**Problem:** POST to `/api/admin/articles` returns 403
**Solution:** Include `X-Admin-Key: dev-admin-key` header (dev) or correct production key. Ensure header name is exact.

### Tests fail

**Problem:** One or more tests fail
**Solution:** Run tests in verbose mode for details:
- Backend: `./mvnw test -X`
- Frontend: `npm test -- --verbose`

## 📞 Support

For issues or questions:
1. Check the troubleshooting section above
2. Review the specification in `.kiro/specs/simple-blogging-site/`
3. Check recent git history for related changes
4. Review test files for usage examples

## 📝 License

This project is provided as-is for educational and personal use.

---

**Last Updated:** October 3, 2026  
**Status:** Ready for Production Deployment  
**Test Coverage:** 104 tests (40 backend + 64 frontend) — All Passing ✅

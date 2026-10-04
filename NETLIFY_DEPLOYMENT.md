# Netlify Deployment Guide

This guide walks you through deploying your blogging site's frontend to Netlify and backend to Railway.

## 🎯 What You'll Deploy

- **Frontend** (React SPA) → **Netlify** (free tier available)
- **Backend** (Spring Boot API) → **Railway** or **Render** (free trial)
- **Database** (PostgreSQL) → **Supabase** (free tier available)

---

## Step 1: Prepare Your Repository

### 1.1 Ensure Git is Initialized
```bash
cd c:\Users\shash\PythonPackage\my_blogging_site
git init
git add .
git commit -m "Initial commit - blogging site ready for deployment"
```

### 1.2 Push to GitHub
1. Create a new repository on [GitHub](https://github.com/new)
   - Repository name: `my-blogging-site` (or your choice)
   - Make it **Public** (free tier requirement for Netlify)
   
2. Push your code:
```bash
git remote add origin https://github.com/YOUR_USERNAME/my-blogging-site.git
git branch -M main
git push -u origin main
```

---

## Step 2: Deploy Backend to Railway

### 2.1 Create Railway Account
1. Go to [Railway.app](https://railway.app)
2. Click **Sign Up**
3. Choose **GitHub** authentication (recommended)
4. Authorize Railway to access your GitHub account

### 2.2 Create a New Project
1. Dashboard → **New Project**
2. Choose **Deploy from GitHub repo**
3. Select `my-blogging-site` repository
4. Railway auto-detects Maven and configures build

### 2.3 Add PostgreSQL Database
1. Dashboard → **Add service** → **PostgreSQL**
2. Railway creates database automatically and provides:
   - `DATABASE_URL` (complete connection string)
   - `PGUSER` (username)
   - `PGPASSWORD` (password)

### 2.4 Configure Environment Variables
In Railway Dashboard → Variables tab, add:

```
DB_URL=<PostgreSQL connection string from DATABASE_URL>
DB_USERNAME=<PGUSER>
DB_PASSWORD=<PGPASSWORD>
ADMIN_KEY=your-secure-admin-key-here-change-this
ALLOWED_ORIGINS=https://your-frontend.netlify.app
```

**Important:** Change `ADMIN_KEY` to something strong and remember it!

### 2.5 Deploy
1. Go to **Deployments** tab
2. Railway auto-builds from `main` branch
3. Build completes in ~5-10 minutes
4. Once deployed, note your backend URL:
   - Format: `https://your-project-random-id.railway.app`
   - Copy this URL (you'll need it for frontend)

**Verify backend is running:**
```bash
curl https://your-backend-url.railway.app/api/articles
# Should return: {"content":[],"totalElements":0,...}
```

---

## Step 3: Deploy Frontend to Netlify

### 3.1 Create Netlify Account
1. Go to [Netlify.com](https://app.netlify.com)
2. Click **Sign up**
3. Choose **GitHub** (recommended)
4. Authorize and connect your GitHub account

### 3.2 Create New Site
1. Dashboard → **Add new site** → **Import an existing project**
2. Select **GitHub** as the Git provider
3. Find and select `my-blogging-site` repository
4. Click **Deploy site**

### 3.3 Configure Build Settings
Netlify might show build configuration. Verify:

| Setting | Value |
|---------|-------|
| **Build command** | `cd frontend && npm run build` |
| **Publish directory** | `frontend/build` |
| **Base directory** | (leave empty) |

### 3.4 Set Environment Variables
Before building:

1. Dashboard → **Site settings** → **Build & deploy** → **Environment**
2. Click **Edit variables**
3. Add variable:
   - **Key:** `REACT_APP_API_URL`
   - **Value:** `https://your-backend-url.railway.app` (from Step 2.5)

### 3.5 Trigger Build
1. Netlify auto-builds when you add environment variables
2. Go to **Deployments** tab
3. Wait for build to complete (typically 2-3 minutes)
4. Once complete, click the deploy link to verify

**Your frontend URL will look like:** `https://your-site-name.netlify.app`

---

## Step 4: Update Backend ALLOWED_ORIGINS

The frontend URL has changed! Update backend:

### 4.1 Update Environment Variable
On Railway Dashboard:
1. Go to **Variables** tab
2. Edit `ALLOWED_ORIGINS`
3. Change to: `https://your-site-name.netlify.app`
4. Save (backend auto-redeploys)

Wait 1-2 minutes for redeploy.

---

## Step 5: Test Your Deployment

### 5.1 Test Frontend
1. Open `https://your-site-name.netlify.app` in browser
2. Verify home page loads with articles
3. Click on an article → should load full content
4. Test search functionality
5. Test submit post → should show success

### 5.2 Test Backend Connection
1. Open browser DevTools → **Network** tab
2. Reload page
3. Look for API calls to `https://your-backend-url.railway.app/api/articles`
4. Should return 200 status (not CORS errors)

### 5.3 Test Admin Functionality
1. Open browser DevTools → **Console**
2. Manually test admin endpoint:
```javascript
fetch('https://your-backend-url.railway.app/api/admin/articles', {
  method: 'POST',
  headers: {
    'X-Admin-Key': 'your-secure-admin-key-here',
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    title: 'Test Article',
    body: 'This is a test',
    category: 'Testing'
  })
})
.then(r => r.json())
.then(console.log)
```

Should return 201 with article details (not 403).

---

## Step 6: Configure Custom Domain (Optional)

### 6.1 On Netlify
1. Dashboard → **Domain settings**
2. Click **Add domain**
3. Enter your custom domain (e.g., `myblog.com`)
4. Follow instructions to update DNS

### 6.2 Update Backend ALLOWED_ORIGINS
Update Railway environment variable:
```
ALLOWED_ORIGINS=https://myblog.com
```

---

## Step 7: Set Up Auto-Deployments

### 7.1 Frontend (Netlify)
- **Automatic:** Netlify auto-deploys when you push to `main` branch
- No additional setup needed

### 7.2 Backend (Railway)
- **Automatic:** Railway auto-deploys when you push to `main` branch
- No additional setup needed

---

## 📝 Environment Variables Reference

### Backend (Railway)

| Variable | Example | Purpose |
|----------|---------|---------|
| `DB_URL` | `postgresql://user:pass@host:5432/db` | PostgreSQL connection |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `secure_password_123` | Database password |
| `ADMIN_KEY` | `your-super-secret-key` | Admin authentication |
| `ALLOWED_ORIGINS` | `https://myblog.netlify.app` | CORS allowed origin |

### Frontend (Netlify)

| Variable | Example | Purpose |
|----------|---------|---------|
| `REACT_APP_API_URL` | `https://your-backend.railway.app` | Backend API URL |

---

## 🔗 Useful Links

- **Netlify Dashboard:** https://app.netlify.com
- **Railway Dashboard:** https://railway.app/dashboard
- **GitHub:** https://github.com
- **Supabase:** https://supabase.com (if using instead of Railway PostgreSQL)

---

## ❌ Troubleshooting

### Frontend loads but no articles show

**Cause:** Backend URL not set or incorrect
- Check Netlify environment variable `REACT_APP_API_URL`
- Verify backend is running on Railway
- Check browser DevTools → Network tab for API errors

**Fix:**
1. On Netlify → Site settings → Environment
2. Verify `REACT_APP_API_URL` is correct
3. Redeploy: Deployments → Trigger deploy → Deploy site

---

### CORS errors in browser

**Cause:** `ALLOWED_ORIGINS` doesn't match frontend URL

**Fix:**
1. On Railway → Variables
2. Update `ALLOWED_ORIGINS` to match frontend URL exactly
3. Wait 2 minutes for backend to redeploy
4. Hard refresh browser (Ctrl+Shift+R)

---

### Admin key doesn't work

**Cause:** Key not set or doesn't match

**Fix:**
1. Remember the `ADMIN_KEY` you set on Railway
2. When testing, ensure header `X-Admin-Key: your-key` matches exactly
3. No spaces or typos

---

### Build fails on Netlify

**Check the logs:**
1. Netlify Dashboard → Deployments → Click failed deploy
2. Expand build logs
3. Look for error messages

**Common causes:**
- `npm install` failed → Check `package.json` for typos
- `npm run build` failed → Check frontend code for syntax errors
- Missing environment variable → Add `REACT_APP_API_URL`

---

### Build fails on Railway

**Check the logs:**
1. Railway Dashboard → Deployments → Click failed build
2. Expand build output
3. Look for Maven or Java errors

**Common causes:**
- Tests failing → Railway runs `./mvnw test` by default
  - Add to `Procfile`: `./mvnw clean package -DskipTests`
- Java version mismatch → Railway defaults to Java 11, you need Java 21
  - Set environment variable: `JAVA_VERSION=21`

---

## ✅ Deployment Checklist

- [ ] Code pushed to GitHub
- [ ] Railway project created
- [ ] PostgreSQL database added to Railway
- [ ] Backend environment variables set on Railway
- [ ] Backend deployed successfully
- [ ] Backend URL copied
- [ ] Netlify account created
- [ ] Frontend site created on Netlify
- [ ] Frontend build command configured: `cd frontend && npm run build`
- [ ] Frontend publish directory: `frontend/build`
- [ ] `REACT_APP_API_URL` set on Netlify
- [ ] Frontend deployed successfully
- [ ] Backend `ALLOWED_ORIGINS` updated to Netlify URL
- [ ] Backend redeployed
- [ ] Frontend loads and displays articles
- [ ] Admin functionality works
- [ ] Like/unlike functionality works
- [ ] Search works
- [ ] Guest post submission works

---

## 🎉 You're Live!

Your blogging site is now deployed and accessible on the internet!

**Frontend:** https://your-site-name.netlify.app  
**Backend API:** https://your-backend-url.railway.app  
**Admin Dashboard:** https://your-site-name.netlify.app/admin (with X-Admin-Key)

Every push to `main` branch will auto-deploy both frontend and backend.

---

**Need help?** Check the main [README.md](README.md) for more information.

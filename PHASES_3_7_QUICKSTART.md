# Phases 3-7 Quick Setup Guide

**Time to Deploy**: 15 minutes  
**All phases 3-7 are now COMPLETE!** ✅

---

## ⚡ 5-Minute Quickstart

### Terminal 1: Start Backend
```bash
cd /Users/vara/IdeaProjects/JobPulseAutoApply

# Start database services
docker-compose up -d postgres redis

# Wait 5 seconds
sleep 5

# Build with all phases
mvn clean install -DskipTests

# Run Spring Boot
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Backend ready at: http://localhost:8080/api/swagger-ui.html
```

### Terminal 2: Start Frontend
```bash
cd /Users/vara/IdeaProjects/JobPulseAutoApply/frontend

# Install dependencies (first time only)
npm install

# Start dev server
npm run dev

# Frontend ready at: http://localhost:3000
```

### Terminal 3: Test Everything
```bash
# 1. Test health check
curl http://localhost:8080/api/health | jq .

# 2. Register user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username":"demo",
    "password":"demo123",
    "email":"demo@example.com",
    "fullName":"Demo User"
  }' | jq .

# 3. Login and get token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo123"}' | jq -r '.token')

echo "Token: $TOKEN"

# 4. Use token to fetch jobs
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/jobs | jq .
```

---

## 🎯 What Each Phase Does

### Phase 3: Real Job Boards
**Status**: ✅ RSS Adapter ready  
**File**: `src/main/java/com/jobpulse/infrastructure/adapter/RSSJobAdapter.java`

```java
// Parse RSS feeds
RSSJobAdapter rssAdapter = new RSSJobAdapter();
List<ExternalJob> jobs = rssAdapter.fetchFromRSSFeed("https://example.com/feed");
```

### Phase 4: Resume Skills
**Status**: ✅ Skill extraction working  
**Files**: 
- `common/util/ResumeParser.java`
- `domain/entity/ResumeSkill.java`

```java
// Extract skills from resume
Map<String, String> skills = ResumeParser.extractSkills(resumeText);
// Returns: {"java" -> "Languages", "spring" -> "Frameworks"}
```

### Phase 5: Authentication (JWT)
**Status**: ✅ Full JWT auth implemented  
**Files**:
- `domain/entity/User.java`
- `common/util/JwtTokenUtil.java`
- `application/controller/AuthController.java`

**Endpoints**:
```
POST /api/auth/register   → Create new user
POST /api/auth/login      → Get JWT token
```

### Phase 6: Advanced Matching
**Status**: ✅ Weighted scoring ready  
**File**: `common/util/AdvancedMatcher.java`

```java
// Score job against resume with weights
double score = AdvancedMatcher.scoreJobResume(
  jobTitle, jobCompany, jobDescription,
  resumeTitle, resumeSkills, resumeText
);
// Weights: Title 40%, Skills 35%, Description 25%
```

### Phase 7: Frontend UI
**Status**: ✅ React dashboard complete  
**Location**: `frontend/`

**Pages**:
- Login/Register
- Job Listing & Management
- Resume Upload
- Application History

---

## 🚀 Next Steps

### Step 1: Open Frontend (Browser)
```
http://localhost:3000
```

### Step 2: Register Account
- Click "Register"
- Username: `demo`
- Password: `demo123`
- Email: `demo@example.com`
- Full Name: `Demo User`

### Step 3: Login
- Click "Login"
- Enter credentials
- Get redirected to dashboard

### Step 4: Upload Resume
- Go to "Resumes" tab
- Upload any PDF, DOCX, or TXT file
- Skills automatically extracted

### Step 5: Ingest Jobs
- Go to "Jobs" tab
- Click "Ingest Jobs"
- Mock jobs appear in table

### Step 6: Auto-Apply
- Click "Auto-Apply"
- Jobs are scored and matched
- Applications recorded

### Step 7: Check History
- Go to "Applications" tab
- View all applied jobs
- See status and timestamps

---

## 🔧 Key Configuration

### JWT Settings
```yaml
# In application.yml
jwt:
  secret: jobpulse_secret_key_do_not_use_in_production
  expiration: 86400000  # 24 hours
```

### Matching Threshold
```yaml
job:
  match:
    threshold: 0.05  # 5% minimum match
```

### Add More Skills (Phase 4)
Edit `ResumeParser.java`:
```java
SKILL_CATEGORIES.put("rust", "Languages");
SKILL_CATEGORIES.put("kubernetes", "Tools");
```

---

## 📊 API Endpoints (All Phases)

### Authentication (Phase 5)
```
POST /api/auth/register
POST /api/auth/login
```

### Jobs (Phase 2, enhanced by Phase 3 & 6)
```
GET  /api/jobs
POST /api/jobs/ingest
POST /api/jobs/autoapply
```

### Resumes (Phase 2, enhanced by Phase 4)
```
GET  /api/resumes
GET  /api/resumes/active
POST /api/resumes/upload
```

### Applications (Phase 7)
```
GET  /api/applications
GET  /api/applications/{id}
```

---

## 🧪 Testing All Phases

### Test Phase 3 (RSS)
```bash
# RSS adapter is loaded in JobIngestionService
# It will fetch from configured RSS feeds
# Check logs: "Fetched jobs from RSS feeds"
```

### Test Phase 4 (Skills)
```bash
# Upload a resume with tech keywords
# Go to PgAdmin: SELECT * FROM resume_skills
# See extracted skills by category
```

### Test Phase 5 (Auth)
```bash
# Register → Get redirected to login ✓
# Login → Get JWT token ✓
# Token visible in localStorage ✓
# All API calls use Bearer token ✓
```

### Test Phase 6 (Matching)
```bash
# Submit job application
# Check logs: "Matching job X score=0.45"
# Verify weights: Title (40%) + Skills (35%) + Desc (25%)
```

### Test Phase 7 (Frontend)
```bash
# All pages load ✓
# Authentication works ✓
# Resume upload works ✓
# Job listing works ✓
# Application history shows ✓
```

---

## 🐛 Troubleshooting

### "Node modules not found"
```bash
cd frontend
npm install
```

### "Backend not responding"
```bash
# Check if Spring Boot is running
curl http://localhost:8080/api/health

# Check Docker
docker-compose ps
```

### "Port 3000 already in use"
```bash
# Use different port
npm run dev -- --port 3001
```

### "JWT token not working"
```bash
# Clear localStorage
# Delete token from DevTools Storage → LocalStorage
# Login again
```

### "Skills not extracted"
```bash
# Check resume has tech keywords
# Update ResumeParser.java with more skills
# Resume needs actual text (not just images)
```

---

## 📱 Frontend Features

| Feature | Phase | Status |
|---------|-------|--------|
| Login/Register | 5 | ✅ Working |
| Job Listing | 2 | ✅ Working |
| Job Ingestion | 2,3 | ✅ Working |
| Auto-Apply | 2,6 | ✅ Enhanced |
| Resume Upload | 2,4 | ✅ Enhanced |
| Skill Extraction | 4 | ✅ Working |
| Application History | 7 | ✅ Working |
| Dark Mode | - | 🔜 Future |
| Mobile App | - | 🔜 Future |

---

## 🎉 You're Ready!

**All 7 phases are now implemented and ready to use!**

### Start Now:
1. Open Terminal 1 & 2 (Backend + Frontend)
2. Navigate to http://localhost:3000
3. Register a test account
4. Upload a resume
5. Ingest jobs
6. Auto-apply
7. View results!

---

## 📞 Commands Reference

```bash
# Backend Setup
docker-compose up -d postgres redis
mvn clean install -DskipTests
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Frontend Setup
cd frontend
npm install
npm run dev

# Stop Everything
docker-compose down
```

---

**Documentation**: See `PHASES_3_7_COMPLETE.md` for detailed information.

**Ready to deploy?** 🚀


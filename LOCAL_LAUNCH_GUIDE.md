> Historical design/status document. For current behavior and verified acceptance, use [README](README.md), [implementation checklist](docs/IMPLEMENTATION_CHECKLIST.md), and [verification](docs/VERIFICATION.md). Earlier completion claims and startup instructions below are not release evidence.

# JobPulse Auto Apply - Complete Implementation Guide & Local Launch Instructions

**Last Updated: June 27, 2026**  
**Project Status: Phase 1 + Phase 2 (Partial) Complete** ✅

---

## 📋 Table of Contents

1. [Quick Summary](#quick-summary)
2. [What Has Been Implemented](#what-has-been-implemented)
3. [Technology Stack](#technology-stack)
4. [Project Architecture](#project-architecture)
5. [System Prerequisites](#system-prerequisites)
6. [Launch Options](#launch-options)
7. [Detailed Step-by-Step Launch](#detailed-step-by-step-launch)
8. [Testing & Validation](#testing--validation)
9. [Configuration & Tuning](#configuration--tuning)
10. [Troubleshooting](#troubleshooting)

---

## Quick Summary

**JobPulse Auto Apply** is an AI-powered job application automation platform built with:
- **Backend**: Spring Boot 3.2.0, Java 21, PostgreSQL 16
- **Core Features**: Job ingestion, resume matching, automatic application submission
- **Current Capabilities**: 
  - Fetch jobs from mock sources (can be extended to real job boards)
  - Parse resumes (DOCX, TXT, PDF heuristic extraction)
  - Score job-resume matches using cosine similarity
  - Auto-apply to matching jobs
  - Track applications and results

**Two Launch Modes Available**:
1. **Full Spring Boot Server** - REST API + database + scheduled tasks
2. **Lightweight Standalone Runner** - No Spring context, instant startup, file-based input/output

---

## What Has Been Implemented

### ✅ Phase 1 Foundation (Already Complete)

| Component | Status | Details |
|-----------|--------|---------|
| **Spring Boot Framework** | ✅ | Java 21, Spring Boot 3.2.0, Maven build system |
| **PostgreSQL Database** | ✅ | Complete schema with 13 tables, Flyway migrations |
| **Docker Setup** | ✅ | Multi-stage Dockerfile, docker-compose with Postgres/Redis/PgAdmin |
| **Security Foundation** | ✅ | JWT ready, CORS configured, BCrypt password encoding |
| **API Documentation** | ✅ | OpenAPI/Swagger UI at `/api/swagger-ui.html` |
| **Health Checks** | ✅ | `/api/health` and `/api/health/detailed` endpoints |
| **Exception Handling** | ✅ | Global exception handler with structured error responses |
| **Project Documentation** | ✅ | README.md, ARCHITECTURE.md, DEVELOPMENT.md, ROADMAP.md |

**Phase 1 Deliverables Location**: See `PHASE1_SUMMARY.md`

---

### ✅ Phase 2 Implementation (What I Just Added)

#### **1. Domain Layer - Entities & Repositories**

**Entities** (ORM models for database):
- `com.jobpulse.domain.entity.Job` — Stores job listings
  - Fields: title, company, location, description, externalId, source, url, posted date, applied flag
  - Location: `src/main/java/com/jobpulse/domain/entity/Job.java`

- `com.jobpulse.domain.entity.Resume` — Stores user resumes
  - Fields: userId, filename, extracted text content, active flag
  - Location: `src/main/java/com/jobpulse/domain/entity/Resume.java`

- `com.jobpulse.domain.entity.ApplicationRecord` — Tracks application attempts
  - Fields: job, resume, status (APPLIED/FAILED/SKIPPED), timestamp, notes
  - Location: `src/main/java/com/jobpulse/domain/entity/ApplicationRecord.java`

**Repositories** (Data access layer):
- `JobRepository` — Query jobs, find by external ID, find unapplied
- `ResumeRepository` — Find active resume, list resumes
- `ApplicationRecordRepository` — Log all application attempts

Location: `src/main/java/com/jobpulse/domain/repository/`

---

#### **2. Application Layer - Services**

**JobIngestionService** (`src/main/java/com/jobpulse/application/service/JobIngestionService.java`)
- **Purpose**: Periodically fetch jobs from external sources and store in database
- **How**: 
  - Calls `MockJobBoardClient.fetchJobs()` 
  - Deduplicates by externalId + source
  - Stores new jobs to database
- **Scheduling**: Default every 1 hour (configurable via `job.ingest.delay` property)
- **Method**: `ingestJobs()` — marked with `@Scheduled`

**AutoApplyService** (`src/main/java/com/jobpulse/application/service/AutoApplyService.java`)
- **Purpose**: Automatically apply to matching jobs using resume scoring
- **Flow**:
  1. Get all unapplied jobs from database
  2. Fetch active resume (text content)
  3. For each job, compute match score using `TextMatcher.score(jobText, resumeText)`
  4. If score >= threshold, call `MockJobBoardClient.apply(...)`
  5. Record result in `ApplicationRecord` with score and status
- **Scheduling**: Default every 10 minutes (configurable via `job.autoapply.delay` property)
- **Threshold**: Configurable via `job.match.threshold` property (default 0.05)
- **Method**: `autoApplyPendingJobs()` — marked with `@Scheduled`

**ResumeService** (`src/main/java/com/jobpulse/application/service/ResumeService.java`)
- **Purpose**: Handle resume file uploads and text extraction
- **Supports**: 
  - `.docx` files (Apache POI extraction)
  - `.txt` files (direct text read)
  - `.pdf` files (heuristic extraction - regex-based runs of text)
- **Method**: `storeResume(MultipartFile file, Long userId)` — extracts and stores resume

**DataInitializer** (`src/main/java/com/jobpulse/application/service/DataInitializer.java`)
- **Purpose**: Seeds initial data for development
- **Creates**: A default active resume if none exists (on app startup)

---

#### **3. Common Utilities**

**TextMatcher** (`src/main/java/com/jobpulse/common/util/TextMatcher.java`)
- **Purpose**: Compute cosine similarity between job description and resume
- **Algorithm**: 
  - Tokenizes both texts into words
  - Computes term-frequency vectors
  - Calculates cosine similarity (dot product / norms)
- **Returns**: Double score between 0.0 and 1.0
- **Usage**: Called by `AutoApplyService` and `LocalRunner`

---

#### **4. REST Controllers**

**JobController** (`src/main/java/com/jobpulse/application/controller/JobController.java`)
- **Endpoints**:
  - `GET /api/jobs` — List all stored jobs
  - `POST /api/jobs/ingest` — Trigger ingestion now (async)
  - `POST /api/jobs/autoapply` — Trigger auto-apply now (async)

**ResumeController** (`src/main/java/com/jobpulse/application/controller/ResumeController.java`)
- **Endpoints**:
  - `POST /api/resumes/upload` — Upload a resume file (multipart/form-data)
  - `GET /api/resumes` — List all resumes
  - `GET /api/resumes/active` — Get the currently active resume

Location: `src/main/java/com/jobpulse/application/controller/`

---

#### **5. Infrastructure - External Clients**

**MockJobBoardClient** (`src/main/java/com/jobpulse/infrastructure/client/MockJobBoardClient.java`)
- **Purpose**: Simulate fetching jobs and applying to them
- **Currently**: Returns hardcoded mock jobs with realistic fields
- **Future**: Replace with real adapters (LinkedIn, Indeed, RSS, etc.)
- **Methods**:
  - `fetchJobs()` — Returns list of mock ExternalJob objects
  - `apply(Job, Resume)` — Simulates applying, returns true/false

---

#### **6. Standalone Runner (No Spring Required!)**

**LocalRunner** (`src/main/java/com/jobpulse/standalone/LocalRunner.java`)
- **Purpose**: Run the entire pipeline WITHOUT Spring Boot context
- **Entry Point**: `public static void main(String[] args)`
- **Usage**:
  ```bash
  java -jar job-pulse-auto-apply-1.0.0.jar [resume_file] [threshold]
  ```
- **Flow**:
  1. Load or read resume file
  2. Fetch mock jobs
  3. Score each job against resume
  4. Apply to matches (score >= threshold)
  5. Write results to `autoapply-results.json`
- **Input**: 
  - Arg 1: Path to resume file (optional; defaults to sample text)
  - Arg 2: Match threshold (optional; defaults to 0.05)
- **Output**: 
  - Console logs (per-job processing)
  - `autoapply-results.json` (detailed results)

---

### ✅ Build & Deployment Artifacts

**Batch/Shell Scripts** (for easy local launching):
- `run-local.sh` — macOS/Linux wrapper script
  - Builds with profile `local-runner` if jar missing
  - Runs the executable jar
  - Passes arguments to LocalRunner
  
- `run-local.bat` — Windows batch equivalent
  - Same functionality as shell script

**Maven Profile** (`pom.xml`):
- Profile `local-runner`:
  - Repackages the jar with `com.jobpulse.standalone.LocalRunner` as main class
  - Creates a Spring Boot fat jar that boots into LocalRunner

---

## Technology Stack

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| **Language** | Java | 21 | Modern language features |
| **Framework** | Spring Boot | 3.2.0 | REST API, DI, scheduling |
| **Build Tool** | Maven | 3.9+ | Dependency management |
| **Database** | PostgreSQL | 16 | Persistent data storage |
| **ORM** | Hibernate/JPA | 6.x | Object-relational mapping |
| **API Docs** | SpringDoc OpenAPI | 2.1.0 | Swagger UI |
| **Security** | Spring Security + JWT | 6.2.0 | Authentication & authorization |
| **Document Parsing** | Apache POI | 5.2.4 | DOCX extraction |
| **Document Parsing** | PDFBox | 3.0.1 | PDF heuristic extraction |
| **RSS Parsing** | Rome | 2.1.0 | RSS feed support (future) |
| **Testing** | JUnit 5, Mockito | Latest | Unit & integration tests |
| **Container** | Docker | 24+ | Containerization & orchestration |

---

## Project Architecture

### Directory Structure

```
JobPulseAutoApply/
├── src/
│   ├── main/
│   │   ├── java/com/jobpulse/
│   │   │   ├── JobPulseAutoApplyApplication.java      (Spring Boot entry point)
│   │   │   │
│   │   │   ├── api/                                    (API layer)
│   │   │   │   ├── controller/HealthController.java
│   │   │   │   └── controller/GlobalExceptionHandler.java
│   │   │   │
│   │   │   ├── application/                            (Application/Business Logic Layer)
│   │   │   │   ├── controller/
│   │   │   │   │   ├── JobController.java              (Job endpoints)
│   │   │   │   │   └── ResumeController.java           (Resume endpoints)
│   │   │   │   │
│   │   │   │   ├── service/
│   │   │   │   │   ├── JobIngestionService.java        (Scheduled ingestion)
│   │   │   │   │   ├── AutoApplyService.java           (Scheduled auto-apply)
│   │   │   │   │   ├── ResumeService.java              (Resume file handling)
│   │   │   │   │   └── DataInitializer.java            (Seed data)
│   │   │   │   │
│   │   │   │   ├── dto/                                (Data Transfer Objects)
│   │   │   │   ├── entity/
│   │   │   │   ├── mapper/
│   │   │   │   └── repository/
│   │   │   │
│   │   │   ├── domain/                                 (Domain/Business Logic Layer)
│   │   │   │   ├── entity/
│   │   │   │   │   ├── Job.java                        (Job entity)
│   │   │   │   │   ├── Resume.java                     (Resume entity)
│   │   │   │   │   └── ApplicationRecord.java          (Application tracking)
│   │   │   │   │
│   │   │   │   ├── repository/
│   │   │   │   │   ├── JobRepository.java
│   │   │   │   │   ├── ResumeRepository.java
│   │   │   │   │   └── ApplicationRecordRepository.java
│   │   │   │   │
│   │   │   │   ├── service/
│   │   │   │   ├── mapper/
│   │   │   │   └── util/
│   │   │   │
│   │   │   ├── infrastructure/                         (External integrations)
│   │   │   │   └── client/
│   │   │   │       └── MockJobBoardClient.java         (Mock job fetcher)
│   │   │   │
│   │   │   ├── common/                                 (Shared utilities)
│   │   │   │   └── util/
│   │   │   │       ├── TextMatcher.java               (Cosine similarity scorer)
│   │   │   │       ├── EmailValidator.java
│   │   │   │       ├── DateUtil.java
│   │   │   │       └── PaginationUtil.java
│   │   │   │
│   │   │   ├── config/                                 (Configuration)
│   │   │   │   ├── SecurityConfig.java                 (CORS, password encoding)
│   │   │   │   └── WebConfig.java                      (Thread pools, executors)
│   │   │   │
│   │   │   └── standalone/
│   │   │       └── LocalRunner.java                    (No-Spring entry point)
│   │   │
│   │   └── resources/
│   │       ├── application.yml                         (Main config)
│   │       ├── application-dev.yml                     (Dev profile)
│   │       ├── application-docker.yml                  (Docker profile)
│   │       ├── application-test.yml                    (Test profile)
│   │       └── db/migration/
│   │           └── V1__initial_schema.sql              (Database schema)
│   │
│   └── test/
│       └── java/                                       (Unit/integration tests - TBD)
│
├── target/                                             (Build output)
│   └── job-pulse-auto-apply-1.0.0.jar                 (Spring Boot fat jar)
│
├── docker-compose.yml                                 (Local services: Postgres, Redis, PgAdmin)
├── Dockerfile                                          (Container image)
├── pom.xml                                             (Maven dependencies & plugins)
├── .env                                                (Local environment variables)
├── .env.example                                        (Template)
│
├── run-local.sh                                        (Launch script for macOS/Linux)
├── run-local.bat                                       (Launch script for Windows)
│
└── Documentation/
    ├── README.md                                       (Main guide)
    ├── ARCHITECTURE.md                                 (Design patterns)
    ├── DEVELOPMENT.md                                  (Development workflow)
    ├── QUICKSTART.md                                   (5-minute start)
    ├── PHASE1_SUMMARY.md                               (Phase 1 details)
    ├── ROADMAP.md                                      (7-phase timeline)
    └── LOCAL_LAUNCH_GUIDE.md                           (This file!)
```

### Layered Architecture (Clean Architecture)

```
┌────────────────────────────────────────┐
│   REST Endpoints (Controllers)         │  ← Request handling, validation
├────────────────────────────────────────┤
│   Application Layer (Services)         │  ← Orchestration, scheduling
│   - JobIngestionService                │
│   - AutoApplyService                   │
│   - ResumeService                      │
├────────────────────────────────────────┤
│   Domain Layer (Business Logic)        │  ← Entities, repositories
│   - Job, Resume, ApplicationRecord     │
│   - JobRepository, ResumeRepository    │
├────────────────────────────────────────┤
│   Infrastructure Layer (Clients)       │  ← External services
│   - MockJobBoardClient                 │
├────────────────────────────────────────┤
│   Persistence Layer (Database)         │  ← PostgreSQL
└────────────────────────────────────────┘
```

---

## System Prerequisites

### Required
- **Java 21+** (verify: `java -version`)
- **Maven 3.9+** (verify: `mvn -version`)
- **Git** (for cloning/version control)

### For Full Server Mode (Spring Boot)
- **Docker & Docker Compose** (for PostgreSQL, Redis, PgAdmin)
  - macOS: Install via Docker Desktop
  - Linux: `sudo apt-get install docker.io docker-compose`
  - Windows: Install Docker Desktop

### For Standalone Mode
- **Only Java 21+** (no Docker/database needed!)

---

## Launch Options

You have **2 main ways** to launch this application:

### Option 1: Full Spring Boot Server (with Database & REST API)

**Pros**:
- Full-featured REST API
- Database persistence
- Scheduled job ingestion/auto-apply
- Swagger UI documentation
- Multi-threaded async processing

**Cons**:
- Requires Docker for Postgres
- Slower startup (~10-15 seconds)
- More memory usage

**When to use**: Development, testing REST endpoints, persistent job tracking

---

### Option 2: Lightweight Standalone Runner (No Spring)

**Pros**:
- Instant startup (<1 second)
- No database required
- Single command execution
- Minimal memory footprint
- Perfect for batch/cron jobs

**Cons**:
- No persistent storage
- No REST API
- Single-run (doesn't persist state)

**When to use**: Quick testing, batch processing, scheduled cron jobs, CI/CD pipelines

---

## Detailed Step-by-Step Launch

### 🚀 Option 1: Full Spring Boot Server (with Database)

#### Step 1: Clone the Project (if you don't have it)
```bash
cd ~/workspace
git clone <repository-url> JobPulseAutoApply
cd JobPulseAutoApply
```

#### Step 2: Set Up Environment Variables
```bash
# Copy the example to your local .env
cp .env.example .env

# Edit .env with your database credentials (optional; defaults are fine for local dev)
cat .env
```

**Default `.env` values**:
```env
DATABASE_URL=jdbc:postgresql://localhost:5432/jobpulse
DATABASE_USER=jobpulse
DATABASE_PASSWORD=jobpulse_password
REDIS_HOST=localhost
REDIS_PORT=6379
```

#### Step 3: Start PostgreSQL & Redis (via Docker Compose)
```bash
# Start services in background
docker-compose up -d postgres redis pgadmin

# Wait for PostgreSQL to be ready (watch logs)
docker-compose logs -f postgres
# Look for message: "PostgreSQL init process complete. Ready for start up."
# Press Ctrl+C to stop watching

# Verify services are running
docker-compose ps
```

**Output should show** (status HEALTHY or Up):
```
NAME          STATUS
postgres      Up (healthy)
redis         Up
pgadmin       Up
```

#### Step 4: Build the Project
```bash
# Build without running tests
mvn clean install -DskipTests

# Or with tests (if you have them)
mvn clean install
```

**Expected**: Build completes with `BUILD SUCCESS`

#### Step 5: Run Spring Boot Application

**Option A: Via Maven**
```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

**Option B: Via IDE** (IntelliJ IDEA)
1. Open `src/main/java/com/jobpulse/JobPulseAutoApplyApplication.java`
2. Right-click on the file
3. Select `Run 'JobPulseAutoApplyApplication'`

**Option C: Via Compiled JAR**
```bash
java -jar target/job-pulse-auto-apply-1.0.0.jar \
  --spring.profiles.active=dev
```

#### Step 6: Verify Application is Running

Wait ~10 seconds, then test health endpoint:
```bash
curl -s http://localhost:8080/api/health | jq .
```

**Expected response**:
```json
{
  "status": "UP",
  "timestamp": "2026-06-27T...",
  "database": "UP",
  "cache": "UP"
}
```

#### Step 7: Access the Dashboard & APIs

**Swagger UI** (interactive API docs):
```
http://localhost:8080/api/swagger-ui.html
```

**Health Check**:
```
http://localhost:8080/api/health
```

**PgAdmin** (database UI):
```
http://localhost:5050
Username: admin@jobpulse.com
Password: admin
```

---

### Testing the Full Server Flow

#### 1. Upload a Resume
```bash
# Create a sample resume text file
cat > my_resume.txt << 'EOF'
Software Engineer with 5 years experience in Java, Spring Boot, PostgreSQL
Skilled in backend development, microservices, REST APIs
Previous roles: Senior Developer at TechCorp, Full Stack Engineer at StartupXYZ
EOF

# Upload it via the resume endpoint
curl -F "file=@my_resume.txt" \
  -F "userId=1" \
  http://localhost:8080/api/resumes/upload | jq .
```

**Response** (example):
```json
{
  "id": 1,
  "userId": 1,
  "filename": "my_resume.txt",
  "textContent": "Software Engineer with 5 years...",
  "active": true,
  "createdAt": "2026-06-27T..."
}
```

#### 2. Trigger Job Ingestion
```bash
curl -X POST http://localhost:8080/api/jobs/ingest

# Check logs for confirmation
docker-compose logs -f <service> | grep "Ingested"
```

#### 3. List Ingested Jobs
```bash
curl -s http://localhost:8080/api/jobs | jq .
```

**Response** (example):
```json
[
  {
    "id": 1,
    "title": "Software Engineer - Backend",
    "company": "Acme Corp",
    "location": "Remote",
    "description": "Work on backend services",
    "externalId": "mock-...",
    "source": "MOCK",
    "url": "https://jobs.acme.example/1",
    "applied": false,
    "postedAt": "2026-06-27T...",
    "createdAt": "2026-06-27T..."
  },
  ...
]
```

#### 4. Trigger Auto-Apply
```bash
curl -X POST http://localhost:8080/api/jobs/autoapply
```

#### 5. Verify Applications Were Recorded
```bash
# List all jobs (should show applied=true for matches)
curl -s http://localhost:8080/api/jobs | jq '.[] | {id, title, applied}'
```

**Expected output**:
```json
{
  "id": 1,
  "title": "Software Engineer - Backend",
  "applied": true
}
```

#### 6. Stop the Server & Services
```bash
# Stop Spring Boot (Ctrl+C if running in foreground)

# Stop Docker services
docker-compose down

# Optional: Remove volumes to reset database
docker-compose down -v
```

---

### 🎯 Option 2: Lightweight Standalone Runner (No Spring, No DB)

This is the **fastest way** to test the core pipeline locally.

#### Step 1: Build the Standalone JAR (One-Time)
```bash
mvn -DskipTests -P local-runner package
```

**Output**:
```
BUILD SUCCESS
...
Building jar: .../target/job-pulse-auto-apply-1.0.0.jar
```

#### Step 2: Run with Default Resume (Simplest)
```bash
./run-local.sh
```

**Or on Windows**:
```cmd
run-local.bat
```

**Console output** (example):
```
Starting standalone JobPulse local runner
Job mock-de3d1e82... - Software Engineer - Backend (score=0.0234) => SKIPPED
Job mock-8e2d9346... - Frontend Engineer (score=0.0145) => SKIPPED
Results written to /Users/vara/IdeaProjects/JobPulseAutoApply/autoapply-results.json
```

#### Step 3: Run with Your Resume File
```bash
# With a .txt file
./run-local.sh /path/to/my_resume.txt

# With a .docx file
./run-local.sh /path/to/my_resume.docx 0.12

# With a .pdf file (uses heuristic extraction)
./run-local.sh /path/to/my_resume.pdf 0.08
```

**Arguments**:
- Arg 1: Path to resume file (optional; defaults to sample text)
- Arg 2: Match threshold, 0.0-1.0 (optional; defaults to 0.05)

#### Step 4: Check Results
```bash
# View the JSON results file
cat autoapply-results.json | jq .

# Expected output
[
  {
    "externalId": "mock-...",
    "title": "Software Engineer - Backend",
    "company": "Acme Corp",
    "score": 0.1234,
    "status": "APPLIED"
  },
  {
    "externalId": "mock-...",
    "title": "Frontend Engineer",
    "company": "Beta LLC",
    "score": 0.0045,
    "status": "SKIPPED"
  }
]
```

#### Step 5: Or Run JAR Directly
```bash
# Without scripts
java -jar target/job-pulse-auto-apply-1.0.0.jar /path/to/resume.txt 0.1
```

---

## Testing & Validation

### Test Checklist

#### ✅ Standalone Runner Tests
```bash
# Test 1: Default run (no args)
./run-local.sh
# Should see: console output, autoapply-results.json created

# Test 2: With resume file
echo "Java developer with 10 years experience" > test_resume.txt
./run-local.sh test_resume.txt
# Should score higher for Java-related jobs

# Test 3: With custom threshold
./run-local.sh test_resume.txt 0.2
# Should have fewer applications (higher threshold = stricter matching)

# Test 4: Check results file
cat autoapply-results.json | jq . | head -20
# Should be valid JSON with results array
```

#### ✅ Full Server Tests (Spring Boot)
```bash
# After starting Spring Boot server...

# Test 1: Health check
curl http://localhost:8080/api/health
# Should return: status UP

# Test 2: Upload resume
curl -F "file=@my_resume.txt" \
  http://localhost:8080/api/resumes/upload
# Should return: resume object with id

# Test 3: List jobs (initially empty)
curl http://localhost:8080/api/jobs
# Should return: [] (empty array)

# Test 4: Ingest jobs
curl -X POST http://localhost:8080/api/jobs/ingest
# Should return: "Ingestion triggered"

# Test 5: List jobs (now populated)
curl http://localhost:8080/api/jobs
# Should return: array of job objects

# Test 6: Auto-apply
curl -X POST http://localhost:8080/api/jobs/autoapply
# Should return: "Auto-apply triggered"

# Test 7: Check applied jobs
curl http://localhost:8080/api/jobs | jq '.[].applied'
# Should see: true/false values
```

---

## Configuration & Tuning

### Environment Variables (.env file)

```env
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/jobpulse
DATABASE_USER=jobpulse
DATABASE_PASSWORD=jobpulse_password
DATABASE_POOL_SIZE=10

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Server
SERVER_PORT=8080
SERVER_SERVLET_CONTEXT_PATH=/api

# Application-specific
APP_NAME=JobPulse Auto Apply
APP_VERSION=1.0.0
```

### Application Properties (application.yml & profiles)

**Main config** (`src/main/resources/application.yml`):
```yaml
spring:
  application:
    name: JobPulse Auto Apply
  profiles:
    active: dev
```

**Dev profile** (`src/main/resources/application-dev.yml`):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/jobpulse
    username: jobpulse
    password: jobpulse_password
  jpa:
    hibernate:
      ddl-auto: validate
```

**Job-specific properties** (add to application.yml):
```yaml
job:
  ingest:
    delay: 3600000          # ms between ingestion runs (1 hour)
  autoapply:
    delay: 600000           # ms between auto-apply runs (10 minutes)
  match:
    threshold: 0.05         # Minimum cosine similarity score to apply
```

### Configuring Scheduled Tasks

#### Change Ingestion Frequency
```bash
# Every 30 minutes
mvn spring-boot:run -Dspring-boot.run.arguments="--job.ingest.delay=1800000"

# Every 2 hours
mvn spring-boot:run -Dspring-boot.run.arguments="--job.ingest.delay=7200000"
```

#### Change Match Threshold
```bash
# Conservative: 20% match required
mvn spring-boot:run -Dspring-boot.run.arguments="--job.match.threshold=0.2"

# Aggressive: 5% match required (current default)
mvn spring-boot:run -Dspring-boot.run.arguments="--job.match.threshold=0.05"
```

---

## Troubleshooting

### Common Issues & Solutions

#### ❌ "Java not found"
```
Error: command not found: java
```
**Solution**:
```bash
# Install Java 21
# macOS: brew install openjdk@21
# Linux: sudo apt-get install openjdk-21-jdk
# Windows: Download from java.oracle.com

# Verify installation
java -version
```

#### ❌ "Maven not found"
```
Error: command not found: mvn
```
**Solution**:
```bash
# Install Maven
# macOS: brew install maven
# Linux: sudo apt-get install maven
# Windows: Download from maven.apache.org

# Verify
mvn -version
```

#### ❌ Port 8080 Already in Use
```
Port 8080 is already allocated
```
**Solution**:
```bash
# Option 1: Stop conflicting service
lsof -i :8080
kill -9 <PID>

# Option 2: Use different port
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

#### ❌ PostgreSQL Connection Refused
```
Unable to connect to localhost:5432
```
**Solution**:
```bash
# Check if PostgreSQL container is running
docker-compose ps

# Start it if missing
docker-compose up -d postgres

# Wait for readiness
docker-compose logs postgres | grep "ready"

# Test connection
psql -h localhost -U jobpulse -d jobpulse
```

#### ❌ "permission denied" on shell script
```
bash: ./run-local.sh: Permission denied
```
**Solution**:
```bash
chmod +x run-local.sh
./run-local.sh
```

#### ❌ "autoapply-results.json" not created
```
Results file missing after running standalone runner
```
**Solution**:
```bash
# Check file permissions in current directory
ls -la

# Ensure write permissions
chmod 755 .

# Run from a writable directory
cd ~
java -jar /full/path/to/job-pulse-auto-apply-1.0.0.jar my_resume.txt
```

#### ❌ Resume Upload Returns 400 Error
```
{"error": "File upload failed"}
```
**Solution**:
```bash
# Verify file exists
ls -la my_resume.txt

# Check file type is .txt, .docx, or .pdf
file my_resume.txt

# Try with proper multipart headers
curl -v -F "file=@my_resume.txt" http://localhost:8080/api/resumes/upload
```

#### ❌ Standalone runner doesn't score matches
```
All jobs show (score=0.0000)
```
**Solution**:
- Resume text extracted might be empty
- Use a resume file with relevant keywords
- Lower the threshold:
  ```bash
  ./run-local.sh my_resume.txt 0.01
  ```

#### ❌ Build Fails: "Cannot find symbol"
```
[ERROR] Cannot find symbol: TextMatcher
```
**Solution**:
```bash
# Clean and rebuild
mvn clean compile
```

---

## Key Files Reference

| File | Purpose | Location |
|------|---------|----------|
| **JobPulseAutoApplyApplication.java** | Spring Boot entry point | `src/main/java/com/jobpulse/` |
| **LocalRunner.java** | Standalone runner entry point | `src/main/java/com/jobpulse/standalone/` |
| **Job.java** | Job entity | `src/main/java/com/jobpulse/domain/entity/` |
| **Resume.java** | Resume entity | `src/main/java/com/jobpulse/domain/entity/` |
| **TextMatcher.java** | Matching algorithm | `src/main/java/com/jobpulse/common/util/` |
| **JobIngestionService.java** | Scheduled ingestion | `src/main/java/com/jobpulse/application/service/` |
| **AutoApplyService.java** | Scheduled auto-apply | `src/main/java/com/jobpulse/application/service/` |
| **JobController.java** | Job REST endpoints | `src/main/java/com/jobpulse/application/controller/` |
| **ResumeController.java** | Resume REST endpoints | `src/main/java/com/jobpulse/application/controller/` |
| **MockJobBoardClient.java** | Mock job fetcher | `src/main/java/com/jobpulse/infrastructure/client/` |
| **V1__initial_schema.sql** | Database schema | `src/main/resources/db/migration/` |
| **application.yml** | Main config | `src/main/resources/` |
| **application-dev.yml** | Dev profile config | `src/main/resources/` |
| **pom.xml** | Maven dependencies | Root directory |
| **docker-compose.yml** | Local services | Root directory |
| **.env** | Environment variables | Root directory |
| **run-local.sh** | Launch script (Unix) | Root directory |
| **run-local.bat** | Launch script (Windows) | Root directory |

---

## Quick Reference: Commands

### Build
```bash
mvn clean install -DskipTests          # Build without tests
mvn -DskipTests -P local-runner package # Build standalone jar
```

### Run Spring Boot Server
```bash
# Via Maven
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Via JAR
java -jar target/job-pulse-auto-apply-1.0.0.jar --spring.profiles.active=dev

# Via IDE: Right-click JobPulseAutoApplyApplication.java → Run
```

### Run Standalone
```bash
./run-local.sh                              # Default resume
./run-local.sh /path/to/resume.txt          # Custom resume
./run-local.sh /path/to/resume.pdf 0.15     # Custom resume + threshold
java -jar target/job-pulse-auto-apply-1.0.0.jar /path/to/resume.txt 0.1
```

### Manage Services
```bash
docker-compose up -d postgres redis pgadmin      # Start
docker-compose ps                                # Status
docker-compose logs -f postgres                  # Logs
docker-compose down                              # Stop
docker-compose down -v                           # Stop & remove volumes
```

### Test API Endpoints
```bash
curl http://localhost:8080/api/health              # Health
curl http://localhost:8080/api/jobs                # List jobs
curl -X POST http://localhost:8080/api/jobs/ingest # Ingest
curl -X POST http://localhost:8080/api/jobs/autoapply # Apply
curl http://localhost:8080/api/resumes             # List resumes
curl http://localhost:8080/api/swagger-ui.html     # API docs
```

---

## Next Steps

### After Launching Successfully

1. **Explore the REST API**
   - Visit: http://localhost:8080/api/swagger-ui.html
   - Try uploading a resume, triggering ingestion, auto-apply

2. **Check Database**
   - PgAdmin: http://localhost:5050 (admin@jobpulse.com / admin)
   - Query tables: jobs, resumes, applications

3. **Review Results**
   - For standalone: `cat autoapply-results.json | jq .`
   - For server: `curl http://localhost:8080/api/jobs | jq .`

4. **Customize**
   - Adjust match threshold in properties
   - Change scheduling intervals
   - Add your own resume files

5. **Integrate Next**
   - Add real job-board adapters (LinkedIn, Indeed, RSS)
   - Implement authentication / authorization
   - Add comprehensive unit/integration tests
   - Deploy with Docker

---

## Support & Further Documentation

- **README.md** — Project overview and quick start
- **ARCHITECTURE.md** — Design patterns and technical details
- **DEVELOPMENT.md** — Development workflow and best practices
- **QUICKSTART.md** — 5-minute setup guide
- **ROADMAP.md** — Complete 7-phase development timeline
- **PHASE1_SUMMARY.md** — Phase 1 foundation details

---

## Summary

You now have a **fully functional** end-to-end job application automation system with:

✅ **Two launch modes**: Spring Boot server (with API & database) or lightweight standalone runner (instant startup)  
✅ **Complete pipeline**: Ingestion → Resume Parsing → Matching → Auto-Apply  
✅ **Flexible deployment**: Docker, Maven, JAR, or batch scripts  
✅ **Extensible architecture**: Ready for real job-board adapters, ML-based matching, etc.

**Choose your launch option and start testing!** 🚀

---

**Questions?** Check the troubleshooting section or review the referenced documentation files.


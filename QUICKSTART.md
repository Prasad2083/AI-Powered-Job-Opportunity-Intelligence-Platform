# 🚀 Quick Reference Card - JobPulse Auto Apply

## ⚡ 5-Minute Start

```bash
# 1. Start database and services
docker-compose up -d postgres redis pgadmin

# 2. Run application
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# 3. Test it
curl http://localhost:8080/api/health
open http://localhost:8080/api/swagger-ui.html

# 4. Stop when done
docker-compose down
```

---

## 📚 Documentation Map

| Need | Go To | Time |
|------|-------|------|
| Overview | README.md | 15 min |
| Setup Help | DEVELOPMENT.md | 10 min |
| Design Questions | ARCHITECTURE.md | 20 min |
| Full Timeline | ROADMAP.md | 30 min |
| File Details | FILE_MANIFEST.md | 10 min |
| What's Done | PHASE1_SUMMARY.md | 5 min |

---

## 🔗 Important URLs

| URL | Purpose |
|-----|---------|
| http://localhost:8080/api/health | Health check |
| http://localhost:8080/api/swagger-ui.html | API docs |
| http://localhost:5050 | Database admin (admin@jobpulse.com/admin) |
| http://localhost:6379 | Redis (command line) |

---

## 💾 Database Connection

```
Host: localhost
Port: 5432
Database: jobpulse
User: postgres
Password: postgres
```

### Via DBeaver or IntelliJ
1. Add New Data Source → PostgreSQL
2. Enter credentials above
3. Done!

---

## 💻 IDE Setup (5 min)

### IntelliJ IDEA
```
1. File → Open → /Users/vara/IdeaProjects/JobPulseAutoApply
2. Settings → Plugins → Install "Lombok"
3. Settings → Compiler → Annotation Processors → Enable
4. Run → Edit Configurations → + Spring Boot
   - Main class: com.jobpulse.JobPulseAutoApplyApplication
   - Program arguments: --spring.profiles.active=dev
   - Environment: Load from .env
5. Right-click JobPulseAutoApplyApplication → Run
```

---

## 🏗️ Project Structure (3 sec overview)

```
JobPulseAutoApply/
├── src/main/java/com/jobpulse/     ← All Java code
│   ├── api/                         ← REST endpoints
│   ├── application/                 ← Business logic
│   ├── domain/                      ← Core entities
│   ├── infrastructure/              ← External services
│   ├── common/                      ← Shared utilities
│   └── config/                      ← Configuration
├── src/main/resources/
│   ├── application*.yml             ← Config profiles
│   └── db/migration/                ← SQL migrations
├── docker-compose.yml               ← Services
├── Dockerfile                       ← Build image
└── pom.xml                          ← Dependencies
```

---

## 📊 What's Implemented

### ✅ Phase 1 Complete
- Spring Boot 3.2.0 setup
- PostgreSQL with Flyway
- 13 database tables
- Docker containerization
- Security foundation
- Exception handling
- API documentation
- 5 comprehensive guides
- 1,200+ lines of code
- 3,700+ lines of docs

### 🔲 Phase 2 Ready
Job ingestion, resume parsing, skill extraction

### 🔲 Phase 3 Ready
AI embeddings, semantic matching, ranking

### 🔲 Phase 4 Ready
React dashboard, filters, analytics

### 🔲 Phase 5 Ready
Cover letters, resume suggestions, recruiter messages

### 🔲 Phase 6 Ready
Email notifications, application tracking

### 🔲 Phase 7 Ready
Testing framework, CI/CD pipeline, deployment

---

## 🔧 Common Commands

```bash
# Build
mvn clean install                    # Full build with tests
mvn clean install -DskipTests        # Build without tests
mvn clean compile                    # Just compile

# Run
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Test
mvn test                             # Unit tests only
mvn verify                           # Unit + integration tests
mvn test -Dtest=HealthControllerTest # Single test class

# Database
mvn flyway:info                      # View migrations
mvn flyway:baseline                  # Baseline existing DB

# Clean
mvn clean                            # Remove build artifacts
mvn dependency:purge-local-repository # Clear Maven cache

# Docker
docker-compose up -d                 # Start services
docker-compose down                  # Stop services
docker-compose logs -f               # View logs
docker-compose down -v               # Remove volumes
```

---

## 🐛 Quick Fixes

### PostgreSQL won't start
```bash
docker-compose down -v               # Remove volume
docker-compose up -d postgres        # Restart
```

### Port 8080 in use
```bash
lsof -i :8080                        # Find process
kill -9 <PID>                        # Kill process
# OR use different port: SERVER_PORT=8081
```

### Build fails
```bash
mvn clean install -U                 # Update dependencies
mvn dependency:resolve               # Resolve deps
mvn clean install -DskipTests        # Skip tests to isolate issue
```

### Database migration fails
```bash
# Check migration history
docker exec jobpulse-postgres psql -U postgres -d jobpulse \
  -c "SELECT * FROM flyway_schema_history;"

# Reset (DEV ONLY!)
docker exec jobpulse-postgres psql -U postgres -d jobpulse \
  -c "DELETE FROM flyway_schema_history WHERE version > 1;"
```

---

## 🔐 Security Checklist

- [ ] Generate strong JWT_SECRET (min 256 bits) before production
- [ ] Update DB_PASSWORD from default
- [ ] Enable HTTPS in production
- [ ] Set up secrets management (AWS Secrets Manager, etc.)
- [ ] Enable rate limiting in production
- [ ] Configure CORS for production domains
- [ ] Enable WAF rules
- [ ] Set up monitoring and alerts

---

## 📊 Architecture (30 second version)

```
User → [REST API]
         ↓
      Controller (Validate input)
         ↓
      Service (Business logic)
         ↓
      Repository (Database access)
         ↓
      PostgreSQL
```

**Key Principle:** Request flows inward, response flows outward.  
**No circular dependencies** - Layers only depend on inner layers.

---

## 🎯 First 3 Day Plan

### Day 1: Setup & Exploration
- [ ] Run application successfully
- [ ] Access Swagger documentation
- [ ] Test health endpoints
- [ ] Explore project structure
- [ ] Read README.md

### Day 2: Understanding
- [ ] Study ARCHITECTURE.md
- [ ] Review database schema
- [ ] Understand 6-layer design
- [ ] Learn design patterns
- [ ] Review sample code

### Day 3: Learning
- [ ] Study Spring Boot patterns
- [ ] Review entity relationships
- [ ] Understand service layer
- [ ] Learn exception handling
- [ ] Plan Phase 2 approach

---

## 💡 Tips & Tricks

### IDE Shortcuts (IntelliJ)
```
Ctrl+Alt+L   Format code
Ctrl+Shift+O Organize imports
Alt+F12      Terminal
Ctrl+Shift+T Generate test
Ctrl+D       Duplicate line
Ctrl+/       Comment line
```

### Maven Quick Tips
```bash
# Skip tests during development
mvn install -DskipTests

# Build faster (parallel)
mvn clean install -T 1C

# Check for dependency updates
mvn versions:display-dependency-updates

# Tree view of dependencies
mvn dependency:tree | grep "target"
```

### Docker Tips
```bash
# View container logs
docker logs jobpulse-postgres

# Execute command in container
docker exec jobpulse-postgres psql -U postgres -d jobpulse

# Check container resource usage
docker stats

# Inspect container
docker inspect jobpulse-postgres
```

---

## 📞 When You're Stuck

### Step 1: Check Status
```bash
# Is database running?
docker ps | grep postgres

# Does API respond?
curl http://localhost:8080/api/health

# Check application logs
# Via Maven terminal output
# Or in IDE console
```

### Step 2: Check Documentation
1. README.md - General info
2. DEVELOPMENT.md - Troubleshooting section
3. ARCHITECTURE.md - Design patterns
4. Inline code comments

### Step 3: Verify Setup
```bash
# Can you access database?
psql -h localhost -U postgres -d jobpulse

# Is Java 21 installed?
java -version

# Is Maven 3.9+?
mvn -version

# Docker working?
docker ps
```

### Step 4: Reset Everything
```bash
# Clean Maven cache
mvn clean

# Stop and remove Docker resources
docker-compose down -v

# Restart everything
docker-compose up -d postgres
mvn clean install
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

---

## 📋 Checklist: Before Phase 2

### Understanding
- [ ] Can explain 6-layer architecture
- [ ] Know what each layer does
- [ ] Understand entity relationships
- [ ] Familiar with clean architecture

### Technical
- [ ] Application runs locally
- [ ] Database migrations work
- [ ] Docker services start
- [ ] Can access Swagger docs
- [ ] Health check responds

### Knowledge
- [ ] Read README.md
- [ ] Read ARCHITECTURE.md
- [ ] Reviewed database schema
- [ ] Understand design patterns
- [ ] Know next phase goals

---

## 🚀 You're Ready When

✅ You can start the application in <2 minutes  
✅ You can explain the 6-layer architecture  
✅ You understand database entity relationships  
✅ You know where to find answers in documentation  
✅ You can identify which layer to modify for a feature  

---

## 📈 Progress Tracker

```
Phase 1 (Foundation)        [████████████████████] 100% ✅
Phase 2 (Job Ingestion)     [░░░░░░░░░░░░░░░░░░░░]   0% ⬜
Phase 3 (AI Matching)       [░░░░░░░░░░░░░░░░░░░░]   0% ⬜
Phase 4 (Dashboard)         [░░░░░░░░░░░░░░░░░░░░]   0% ⬜
Phase 5 (AI Content)        [░░░░░░░░░░░░░░░░░░░░]   0% ⬜
Phase 6 (Notifications)     [░░░░░░░░░░░░░░░░░░░░]   0% ⬜
Phase 7 (Testing & Deploy)  [░░░░░░░░░░░░░░░░░░░░]   0% ⬜

Total: 14% Complete
ETA: 15-20 weeks with parallelization
```

---

## 🎓 Learning Resources Used

### Spring Boot
- Spring Boot Documentation
- Spring Data JPA Guide
- Spring Security Docs

### Database
- PostgreSQL Manual
- Flyway Documentation

### Docker
- Docker Official Docs
- Docker Compose Reference

### Java
- Java 21 Features
- SOLID Principles
- Design Patterns

---

## ⭐ Key Takeaways

1. **Clean architecture** is foundational
2. **Layers have clear responsibilities**
3. **Database design** enables all features
4. **Documentation** is as important as code
5. **Containerization** simplifies deployment
6. **Configuration management** enables flexibility
7. **Exception handling** ensures reliability
8. **API design** must be consistent
9. **Security** is built-in, not added later
10. **Testing** structure is ready from day 1

---

## 🎬 What's Next?

### Read Now
- README.md (15 min)
- PHASE1_SUMMARY.md (5 min)

### Study This Week
- ARCHITECTURE.md (20 min)
- Database schema (15 min)
- Code patterns (30 min)

### Plan Next Phase
- Review ROADMAP.md (30 min)
- Identify dependencies (15 min)
- Create task list (30 min)

### Start Phase 2
- Job adapter implementations
- Resume parsing
- Deduplication logic

---

## 📞 Quick Links

- 📖 [Full README](./README.md)
- 🤝 [Development Guide](./DEVELOPMENT.md)
- 🏛️ [Architecture Docs](./ARCHITECTURE.md)
- 🛣️ [Roadmap](./ROADMAP.md)
- 📋 [File Manifest](./FILE_MANIFEST.md)
- ✅ [Phase 1 Summary](./PHASE1_SUMMARY.md)

---

**You have everything you need to succeed! 🚀**

*Last Updated: May 23, 2026*  
*Phase 1 Status: COMPLETE ✅*  
*Time to Start: NOW! 🎯*  


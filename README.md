# JobPulse Auto Apply - AI-Powered Job Opportunity Intelligence Platform

## 📋 Project Overview

**JobPulse Auto Apply** is an enterprise-grade, production-ready AI-powered platform designed to revolutionize how senior QA engineers, SDETs, and automation professionals discover, analyze, and apply to job opportunities.

### 🎯 Core Mission
Automatically discover newly posted jobs, analyze them against your resume using AI embeddings, rank opportunities, and generate tailored application content—all while respecting platform terms of service and ethical automation practices.

### 👥 Target Professionals
- Senior QA Automation Engineers
- SDETs (Software Development Engineers in Test)
- Senior Automation Engineers
- Software Development Engineers in Test
- AI Test Automation Engineers
- Quality Engineering Consultants

### 🛠️ Target Roles
- AI QE Engineer
- SDET
- Senior Automation Engineer
- Software Development Engineer in Test
- AI Test Automation Engineer
- Quality Engineering Consultant

---

## 🏗️ Architecture Overview

### Platform Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Frontend (React + TypeScript)            │
│           Modern Dashboard | Job Exploration | Analytics     │
└──────────────────────┬──────────────────────────────────────┘
                       │ REST API (OpenAPI/Swagger)
┌──────────────────────▼──────────────────────────────────────┐
│                 Spring Boot Backend (3.2.0)                  │
│  Clean Architecture | Microservices | Production Ready       │
├───────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ API Layer (Controllers) - REST Endpoints                │ │
│  ├─────────────────────────────────────────────────────────┤ │
│  │ Application Layer (Services) - Business Logic           │ │
│  ├─────────────────────────────────────────────────────────┤ │
│  │ Domain Layer (Entities) - Core Domain Models            │ │
│  ├─────────────────────────────────────────────────────────┤ │
│  │ Infrastructure Layer (Repository) - Data Access        │ │
│  └─────────────────────────────────────────────────────────┘ │
└───────────────────────┬───────────────────────────────────────┘
                        │
        ┌───────────────┼──────────────────┐
        │               │                  │
┌───────▼────────┐ ┌───▼────────────┐ ┌──▼────────────┐
│  PostgreSQL    │ │  Redis Cache   │ │  AI Service   │
│  (PostgreSQL16)│ │  (Caching)     │ │  (Python)     │
└────────────────┘ └────────────────┘ └───────────────┘
```

### Phase 1 - Foundation (✅ COMPLETED)
Establish the project foundation, architecture, and all necessary infrastructure.

**What's Included:**
- ✅ Enterprise-grade Spring Boot setup
- ✅ PostgreSQL database schema with Flyway migrations
- ✅ Docker & Docker Compose configuration
- ✅ Security configuration (CORS, password encoding)
- ✅ Global exception handling
- ✅ Common DTOs and utilities
- ✅ OpenAPI/Swagger documentation setup
- ✅ Configuration management (application.yml, profiles)
- ✅ Health check endpoints

**Not Included (Phases 2-7):**
- Job ingestion APIs
- Resume parsing and management
- AI embeddings and semantic matching
- Dashboard UI
- AI-generated recommendations
- Notification system
- Application tracking
- Deployment and CI/CD

---

## 📁 Project Structure

```
JobPulseAutoApply/
├── src/
│   ├── main/
│   │   ├── java/com/jobpulse/
│   │   │   ├── JobPulseAutoApplyApplication.java    (Main Entry Point)
│   │   │   ├── api/
│   │   │   │   ├── controller/                      (REST Endpoints)
│   │   │   │   ├── dto/                             (Data Transfer Objects)
│   │   │   │   └── mapper/                          (Entity <-> DTO Mapping)
│   │   │   ├── application/
│   │   │   │   ├── service/                         (Business Logic)
│   │   │   │   ├── dto/                             (Application-level DTOs)
│   │   │   │   └── event/                           (Event Publishing)
│   │   │   ├── domain/
│   │   │   │   ├── entity/                          (JPA Entities)
│   │   │   │   ├── repository/                      (Data Access)
│   │   │   │   ├── event/                           (Domain Events)
│   │   │   │   └── service/                         (Domain Services)
│   │   │   ├── infrastructure/
│   │   │   │   ├── client/                          (External API Clients)
│   │   │   │   ├── cache/                           (Caching Layer)
│   │   │   │   └── util/                            (Infrastructure Utilities)
│   │   │   ├── common/
│   │   │   │   ├── exception/                       (Custom Exceptions)
│   │   │   │   ├── dto/                             (Common DTOs)
│   │   │   │   ├── util/                            (Common Utilities)
│   │   │   │   └── constant/                        (Constants)
│   │   │   └── config/
│   │   │       ├── SecurityConfig.java              (Security & CORS)
│   │   │       └── WebConfig.java                   (Web Configuration)
│   │   └── resources/
│   │       ├── application.yml                      (Main Config)
│   │       ├── application-dev.yml                  (Dev Profile)
│   │       ├── application-test.yml                 (Test Profile)
│   │       └── db/
│   │           └── migration/
│   │               └── V1__initial_schema.sql       (Database Schema)
│   └── test/java/com/jobpulse/
│       └── (Unit & Integration Tests - Phase 7)
├── docker-compose.yml                               (Docker Services)
├── Dockerfile                                       (Application Image)
├── pom.xml                                         (Maven Dependencies)
├── .env                                            (Local Configuration)
├── .env.example                                    (Configuration Template)
└── README.md                                       (This File)
```

---

## 🚀 Quick Start

### Prerequisites
- **Java 21+** (Eclipse Temurin or OpenJDK)
- **Maven 3.9+**
- **Docker & Docker Compose**
- **PostgreSQL 16** (or via Docker)
- **Git**

### Option 1: Local Development (with Docker Database)

#### 1️⃣ Clone Repository
```bash
cd /Users/vara/IdeaProjects/JobPulseAutoApply
```

#### 2️⃣ Start PostgreSQL & Dependencies
```bash
docker-compose up -d postgres redis pgadmin
```

Wait for PostgreSQL to be healthy:
```bash
docker ps
# Look for 'jobpulse-postgres' with status 'healthy'
```

#### 3️⃣ Build the Project
```bash
mvn clean install
```

#### 4️⃣ Run the Application
```bash
# Using Maven
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Or using IDE (IntelliJ IDEA)
# Run: JobPulseAutoApplyApplication.java
```

#### 5️⃣ Access the Application
- **API**: http://localhost:8080/api
- **Swagger/OpenAPI**: http://localhost:8080/api/swagger-ui.html
- **Health Check**: http://localhost:8080/api/health
- **PgAdmin**: http://localhost:5050 (admin@jobpulse.com / admin)

### Option 2: Full Docker Setup

```bash
# Build and start all services
docker-compose up --build

# Check logs
docker-compose logs -f jobpulse-backend

# Stop all services
docker-compose down

# Remove volumes
docker-compose down -v
```

---

## 🗄️ Database Schema

### ERD (Entity Relationship Diagram)

```
┌──────────────┐
│    Users     │◄────┐
└──────────────┘     │
       │             │
       ├─────┬───────┤
       │     │       │
       ▼     ▼       ▼
   ┌─────────────────────────┐
   │  Resumes                │
   │  - User-specific        │
   │  - Embeddings           │
   │  - Skills/Experience    │
   └─────────────────────────┘
               │
               ▼
   ┌─────────────────────────┐
   │  Job Embeddings         │
   │  - Vector similarity    │
   └─────────────────────────┘
               │
               ▼
   ┌──────────────────────────────────┐
   │  Jobs                            │
   │  - From multiple sources         │
   │  - Last 24 hours filtering       │
   │  - Deduplication                 │
   └──────────────────────────────────┘
               │
       ┌───────┴───────┬──────────────┐
       │               │              │
       ▼               ▼              ▼
   ┌─────────┐  ┌──────────────┐  ┌──────────────┐
   │Job Match│  │Applications  │  │SavedJobs     │
   │Analysis │  │Tracking      │  │Bookmarks     │
   └─────────┘  └──────────────┘  └──────────────┘
```

### Key Tables

| Table | Purpose | Phase |
|-------|---------|-------|
| `users` | User authentication & profile | Phase 1 ✅ |
| `resumes` | User resume storage & embeddings | Phase 2 |
| `job_sources` | Job source configuration | Phase 2 |
| `jobs` | Job listings from all sources | Phase 2 |
| `job_embeddings` | Vector embeddings for semantic search | Phase 3 |
| `job_matches` | AI analysis results | Phase 3 |
| `applications` | User application history | Phase 6 |
| `application_events` | Application status tracking | Phase 6 |
| `saved_jobs` | Bookmarked jobs | Phase 4 |
| `analytics_events` | User behavior tracking | Phase 8 |

### Database Initialization

**Flyway Migrations** handle schema creation automatically on startup.

Location: `src/main/resources/db/migration/`

To add new tables or columns in future phases, create a new migration file:
```
V2__add_new_table.sql
V3__modify_existing_table.sql
```

---

## 🔐 Security

### Authentication & Authorization (Phase 5)
- JWT token-based authentication
- Refresh token mechanism
- Role-based access control (User, Admin)
- OAuth2 support (future)

### Current Phase 1 Security
- ✅ CORS configuration for frontend integration
- ✅ Password encoding with BCrypt (strength: 12)
- ✅ Input validation framework
- ✅ Global exception handling

### Security Best Practices
- Never commit `.env` with real credentials
- Use environment variables for sensitive data
- Implement API rate limiting (configured)
- Validate all user inputs
- Log security events
- Use HTTPS in production

---

## 📊 Configuration

### Environment Variables

Copy `.env.example` to `.env` and update:

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=jobpulse
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Server
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev

# JWT Security
JWT_SECRET=your-256-bit-secret-key
JWT_EXPIRATION_MS=86400000

# AI Service
AI_SERVICE_URL=http://localhost:8081
OPENAI_API_KEY=sk-...
```

### Application Profiles

```bash
# Development (verbose logging, db ddl-auto=create-drop)
spring.profiles.active=dev

# Production (minimal logging, db ddl-auto=validate)
spring.profiles.active=prod

# Test (H2 in-memory database)
spring.profiles.active=test

# Docker (PostgreSQL container URLs)
spring.profiles.active=docker
```

---

## 🧪 Testing (Phase 7)

### Test Structure
```
src/test/java/com/jobpulse/
├── api/controller/
│   └── *ControllerTest.java
├── application/service/
│   └── *ServiceTest.java
└── integration/
    └── *IntegrationTest.java
```

### Run Tests
```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=HealthControllerTest

# Integration tests
mvn verify

# With coverage
mvn test jacoco:report
```

---

## 📚 API Documentation

### Swagger/OpenAPI

Access at: `http://localhost:8080/api/swagger-ui.html`

### Health Check Endpoints

#### Basic Health
```bash
GET /api/health
```

Response:
```json
{
  "status": "UP",
  "applicationName": "JobPulse Auto Apply",
  "version": "1.0.0",
  "timestamp": "2026-05-23T10:30:00Z",
  "message": "Application is running"
}
```

#### Detailed Health
```bash
GET /api/health/detailed
```

Response:
```json
{
  "status": "UP",
  "applicationName": "JobPulse Auto Apply",
  "version": "1.0.0",
  "timestamp": "2026-05-23T10:30:00Z",
  "uptime": 3600000,
  "memoryUsage": {
    "totalMemory": 1024000000,
    "freeMemory": 512000000,
    "usedMemory": 512000000,
    "maxMemory": 2048000000
  }
}
```

---

## 🛠️ Development Workflow

### IDE Setup (IntelliJ IDEA)

1. **Open Project**
   ```
   File → Open → /Users/vara/IdeaProjects/JobPulseAutoApply
   ```

2. **Install Lombok Plugin**
   ```
   IntelliJ IDEA → Settings → Plugins → Search "Lombok" → Install
   ```

3. **Enable Annotation Processing**
   ```
   Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   → Enable Annotation Processing
   ```

4. **Configure Run Configuration**
   - Program arguments: `--spring.profiles.active=dev`
   - Environment variables: Load from `.env`

5. **Database Connection (DBeaver or IntelliJ)**
   ```
   Host: localhost
   Port: 5432
   Database: jobpulse
   User: postgres
   Password: postgres
   ```

### Code Style & Standards

- **Language**: Java 21 with records and sealed classes
- **Build Tool**: Maven 3.9+
- **Code Format**: IDEA Format (Ctrl+Alt+L)
- **Naming Convention**: camelCase for variables, PascalCase for classes
- **Documentation**: Javadoc for all public methods
- **Logging**: Use SLF4J with Logback

### Git Workflow

```bash
# Create feature branch
git checkout -b feature/your-feature-name

# Commit with meaningful messages
git commit -m "feat: add new API endpoint for job search"

# Push and create PR
git push origin feature/your-feature-name
```

---

## 🔄 Build & Deployment

### Maven Commands

```bash
# Clean build
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Build JAR only
mvn clean package

# Run application
mvn spring-boot:run

# Generate Swagger/OpenAPI docs
mvn springdoc-openapi-maven-plugin:generate-schema
```

### Docker Build

```bash
# Build Docker image
docker build -t jobpulse-backend:1.0.0 .

# Run container
docker run -p 8080:8080 \
  -e DB_HOST=postgres \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  jobpulse-backend:1.0.0
```

### Production Deployment (Future)

Will include:
- GitHub Actions CI/CD pipeline
- Container registry (Docker Hub/ECR)
- Kubernetes deployment manifests
- Terraform infrastructure as code
- Monitoring and alerting setup

---

## 📦 Dependencies Overview

### Core Framework
- **Spring Boot 3.2.0** - Application framework
- **Spring Data JPA** - ORM & data access
- **Spring Security** - Authentication & authorization
- **Spring WebFlux** - Async HTTP client

### Database
- **PostgreSQL 16** - Primary database
- **Flyway 9.x** - Database migrations
- **Hibernate** - JPA implementation

### API & Documentation
- **SpringDoc OpenAPI** - OpenAPI/Swagger documentation
- **Jackson** - JSON processing

### Document Processing (Phase 2)
- **Apache PDFBox** - PDF parsing
- **Apache POI** - Word document parsing

### Utilities
- **Lombok** - Boilerplate reduction
- **Apache Commons** - Utilities library

### Testing
- **JUnit 5** - Unit testing
- **Mockito** - Mocking library
- **TestContainers** - Docker-based testing

---

## 🎯 Next Steps (Phases 2-7)

### Phase 2: Job Ingestion Engine
- Implement job source adapters (LinkedIn, Indeed, etc.)
- Job fetching and deduplication
- Resume upload and parsing
- Skill extraction

### Phase 3: AI Semantic Matching
- OpenAI embeddings integration
- Semantic similarity calculation
- Job matching service
- ATS optimization scoring

### Phase 4: Dashboard UI
- React TypeScript frontend
- Modern dashboard design
- Job filter and search
- Analytics charts

### Phase 5: AI Recommendations
- Personalized cover letter generation
- Resume improvement suggestions
- Recruiter message templates
- Interview probability prediction

### Phase 6: Notifications & Application Tracking
- Email notification service
- Daily digest system
- Application tracking
- Follow-up reminders

### Phase 7: Testing & Deployment
- Comprehensive unit tests
- Integration tests
- GitHub Actions CI/CD
- Production deployment

---

## 📋 Troubleshooting

### PostgreSQL Connection Failed
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# View PostgreSQL logs
docker logs jobpulse-postgres

# Restart PostgreSQL
docker-compose restart postgres
```

### Maven Build Failures
```bash
# Clear Maven cache
mvn clean

# Update dependencies
mvn dependency:resolve

# Skip tests temporarily
mvn clean install -DskipTests
```

### Port Already in Use
```bash
# Change port in application.yml or .env
SERVER_PORT=8081

# Or kill process on port 8080
lsof -i :8080
kill -9 <PID>
```

### Flyway Migration Issues
```bash
# Baseline existing database
mvn flyway:baseline

# View migration history
docker exec jobpulse-postgres psql -U postgres -d jobpulse -c "SELECT * FROM flyway_schema_history;"
```

---

## 📞 Support & Documentation

### Resources
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA Guide](https://spring.io/projects/spring-data-jpa)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [OpenAPI 3.0 Specification](https://spec.openapis.org/oas/v3.0.3)
- [Docker Documentation](https://docs.docker.com/)

### Common Questions

**Q: How do I change the database?**
A: Update database connection properties in `application-{profile}.yml`

**Q: Can I use MySQL instead of PostgreSQL?**
A: Yes, but you'll need to adjust dialect and migration syntax. Not recommended for production.

**Q: How do I enable HTTPS?**
A: Configure Spring Security SSL in `application-prod.yml` (Phase 5)

**Q: How do I increase database connection pool?**
A: Update `DB_POOL_SIZE` in `.env` (default: 20)

---

## 📄 License

Proprietary - JobPulse Auto Apply Platform

---

## 🤝 Contributing

This is a demonstration project built as an enterprise-grade solution for AI-powered job discovery and application assistance.

### Code Quality Standards
- ✅ Clean architecture principles
- ✅ SOLID design principles
- ✅ Comprehensive documentation
- ✅ Production-ready error handling
- ✅ Security best practices
- ✅ Scalable microservice design

---

## 🎓 Learning Outcomes

By studying this platform, you'll learn:

1. **Enterprise Spring Boot Architecture**
   - Clean architecture pattern
   - Layered application design
   - Repository pattern
   - Service-oriented architecture

2. **Database Design**
   - Entity relationship modeling
   - PostgreSQL normalization
   - Flyway migrations
   - Indexing strategy

3. **API Development**
   - RESTful API design
   - OpenAPI/Swagger documentation
   - Error handling & validation
   - Pagination & filtering

4. **Security**
   - JWT authentication
   - Password hashing
   - CORS configuration
   - Input validation

5. **DevOps & Containerization**
   - Docker & Docker Compose
   - Multi-stage builds
   - Health checks
   - Environment configuration

6. **Testing & Quality**
   - Unit testing patterns
   - Integration testing
   - Test containers
   - Code coverage

---

## 📈 Performance Considerations

### Optimization Strategies
- **Caching**: Redis for frequent queries
- **Database Indexing**: Strategic indexes on search fields
- **Connection Pooling**: HikariCP with optimal settings
- **Async Processing**: Non-blocking operations
- **Pagination**: Limit result sets
- **Batch Processing**: Bulk operations for job ingestion

### Scalability
- **Horizontal Scaling**: Stateless architecture
- **Load Balancing**: Multiple backend instances
- **Database Replication**: Read replicas
- **Message Queue**: Async job processing (RabbitMQ/Kafka later)
- **Microservices**: Python AI service as separate deployment

---

## 🎬 Getting Help

1. **Check logs**: `docker-compose logs -f jobpulse-backend`
2. **Review database**: Connect via PgAdmin (localhost:5050)
3. **Test endpoints**: Use Swagger UI (localhost:8080/api/swagger-ui.html)
4. **Debug code**: Use IntelliJ IDEA debugger
5. **Ask questions**: Review Phase 1 architecture documentation

---

## ✨ What's Next?

You now have a **production-grade foundation** ready for:
- ✅ Phase 2: Job ingestion and resume parsing
- ✅ Phase 3: AI embeddings and semantic matching
- ✅ Phase 4: Modern React dashboard
- ✅ Phase 5: AI-powered recommendations
- ✅ Phase 6: Notification system
- ✅ Phase 7: Testing and deployment

**The architecture is scalable, maintainable, and ready for enterprise use!**

---

**Built with ❤️ for SDET and QA Automation Professionals**

*Last Updated: May 23, 2026*
*Version: 1.0.0 (Phase 1)*


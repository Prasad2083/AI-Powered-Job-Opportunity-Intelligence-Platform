# Architecture Design Document

## 🏛️ Architectural Overview

JobPulse Auto Apply follows **Clean Architecture** principles, separating concerns into distinct layers with clear dependencies flowing inward.

```
┌─────────────────────────────────────────────────────────────┐
│                      Presentation Layer                      │
│                  (REST Controllers, DTOs)                    │
└────────────────────────┬────────────────────────────────────┘
                         │ Depends on
┌────────────────────────▼────────────────────────────────────┐
│                   Application Layer                          │
│              (Services, Use Cases, Mappers)                  │
└────────────────────────┬────────────────────────────────────┘
                         │ Depends on
┌────────────────────────▼────────────────────────────────────┐
│                     Domain Layer                             │
│         (Entities, Repositories, Domain Services)            │
└────────────────────────┬────────────────────────────────────┘
                         │ Depends on
┌────────────────────────▼────────────────────────────────────┐
│                Infrastructure Layer                          │
│       (JPA Implementation, External Clients, Cache)          │
└─────────────────────────────────────────────────────────────┘
```

---

## 📦 Layer Breakdown

### 1. **Presentation Layer** (`api/`)

**Responsibility**: Handle HTTP requests/responses

**Components**:
- `controller/` - REST API endpoints
  - `HealthController.java` - Application health monitoring
  - (Future: JobController, ResumeController, ApplicationController, etc.)
- `dto/` - Data Transfer Objects for API contracts
- `mapper/` - Convert DTOs ↔ Domain Entities

**Principles**:
- Controllers are thin (2-5 lines of business logic)
- All validation happens here via @Valid
- No business logic in controllers
- Returns consistent ApiResponse wrapper

**Example Pattern**:
```java
@RestController
@RequestMapping("/jobs")
public class JobController {
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobDTO>> getJob(@PathVariable Long id) {
        JobDTO job = jobService.getJobById(id);  // Service call
        return ResponseEntity.ok(ApiResponse.success(job));
    }
}
```

---

### 2. **Application Layer** (`application/`)

**Responsibility**: Orchestrate business logic, coordinate between domain and infrastructure

**Components**:
- `service/` - Use cases and business orchestration
  - Coordinates multiple domain operations
  - Handles transactions
  - Error handling and logging
- `dto/` - Internal DTOs (different from API DTOs)
- `event/` - Application events for async processing
- `mapper/` - Complex transformations

**Principles**:
- One service per domain concept (UserService, JobService, etc.)
- Services are transactional
- Services depend only on repositories and other services
- No framework-specific code in services

**Example Pattern**:
```java
@Service
@Transactional
public class JobMatchingApplicationService {
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final JobMatchDomainService jobMatchDomainService;
    
    public JobMatch matchJobWithResume(Long jobId, Long resumeId) {
        Job job = jobRepository.findByIdOrThrow(jobId);
        Resume resume = resumeRepository.findByIdOrThrow(resumeId);
        
        // Delegate to domain service for core logic
        JobMatch match = jobMatchDomainService.performMatching(job, resume);
        
        // Save result
        return jobRepository.save(match);
    }
}
```

---

### 3. **Domain Layer** (`domain/`)

**Responsibility**: Core business logic, entity definitions, data access contracts

**Components**:
- `entity/` - JPA entities (core domain models)
  - User, Resume, Job, JobMatch, Application, etc.
  - Clean entity design with proper relationships
  - Minimal business logic
- `repository/` - Data access interfaces
  - JpaRepository extensions
  - Custom query methods
  - Named queries
- `service/` - Domain-specific business logic
  - Pure business rules
  - Calculations and validations
  - Framework-agnostic

**Principles**:
- Entities represent core domain concepts
- Repositories define data access contracts
- No @Transactional on entities
- Domain services contain pure business logic

**Example Pattern**:
```java
@Entity
@Table(name = "jobs")
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private JobSource source;
    
    // Domain methods (pure logic)
    public boolean isNewJob() {
        return ChronoUnit.DAYS.between(this.postedDate, Instant.now()) <= 1;
    }
}
```

---

### 4. **Infrastructure Layer** (`infrastructure/`)

**Responsibility**: Technical implementations and external service integration

**Components**:
- `client/` - External API clients
  - OpenAI API client (Phase 3)
  - Job source API adapters (Phase 2)
  - HTTP clients for third-party services
- `cache/` - Caching implementations
  - Cache managers
  - Cache-aside patterns
- `util/` - Infrastructure utilities

**Principles**:
- Adapts external systems to domain model
- Hides technical complexity
- Implements interfaces defined in domain layer

**Example Pattern**:
```java
@Component
public class OpenAIEmbeddingClient {
    private final RestTemplate restTemplate;
    
    public Vector generateEmbedding(String text) {
        try {
            OpenAIRequest request = new OpenAIRequest(text);
            OpenAIResponse response = restTemplate.postForObject(
                apiUrl, request, OpenAIResponse.class
            );
            return convertToVector(response);
        } catch (Exception e) {
            throw new ExternalServiceException("OpenAI", e);
        }
    }
}
```

---

### 5. **Common Layer** (`common/`)

**Responsibility**: Shared utilities, exceptions, and constants

**Components**:
- `exception/` - Custom exception hierarchy
  - JobPulseException (base)
  - ResourceNotFoundException
  - ValidationException
  - ExternalServiceException
- `dto/` - Common DTOs (PageResponse, ApiResponse)
- `util/` - Utility classes
  - EmailValidator
  - DateUtil
  - PaginationUtil
- `constant/` - Application constants

---

## 🔄 Request Flow Diagram

```
HTTP Request
     │
     ▼
Controller (Validate Input with @Valid)
     │
     ├─► Global Exception Handler (on error)
     │
     ▼
Service Layer (Business Logic)
     │
     ├─► Validate Business Rules
     ├─► Coordinate Operations
     ├─► Publish Events (async)
     │
     ▼
Repository (Data Access)
     │
     ▼
Database
     │
     ├─ Query/Insert/Update/Delete
     │
     ▼
Service Layer (Process Result)
     │
     ▼
Mapper (Entity → DTO)
     │
     ▼
Controller (Wrap in ApiResponse)
     │
     ▼
HTTP Response (JSON)
```

---

## 🏗️ Design Patterns Used

### 1. **Repository Pattern**
Abstracts data access, allows easy testing with mocks

```java
public interface JobRepository extends JpaRepository<Job, Long> {
    Optional<Job> findBySourceJobId(String sourceJobId);
    Page<Job> findByCompanyName(String company, Pageable pageable);
}
```

### 2. **Service Layer Pattern**
Encapsulates business logic, promotes reusability

```java
@Service
public class JobService {
    public List<Job> findRecentJobs() {
        return jobRepository.findByPostedDateAfter(
            Instant.now().minus(1, ChronoUnit.DAYS)
        );
    }
}
```

### 3. **DTO Pattern**
Decouples API contracts from internal domain models

```java
// API Layer
public class JobDTO {
    private String title;
    private String companyName;
}

// Domain Layer
public class Job {  // Different structure, additional fields
    private String title;
    private String description;
    private JobSource source;
}
```

### 4. **Mapper Pattern**
Converts between DTOs and Entities

```java
@Component
public class JobMapper {
    public JobDTO toDTO(Job entity) {
        return JobDTO.builder()
            .title(entity.getTitle())
            .companyName(entity.getSource().getName())
            .build();
    }
}
```

### 5. **Exception Hierarchy Pattern**
Consistent error handling across application

```java
JobPulseException (base)
├── ResourceNotFoundException (404)
├── ValidationException (400)
└── ExternalServiceException (503)
```

### 6. **Transactional Pattern**
Ensures data consistency

```java
@Service
@Transactional  // All methods transactional
public class ResumeService {
    
    @Transactional(readOnly = true)  // Override for read operations
    public Resume getResume(Long id) { ... }
}
```

### 7. **Global Exception Handler Pattern**
Centralized exception handling

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(JobPulseException.class)
    public ResponseEntity<ErrorResponse> handle(JobPulseException ex) {
        return ResponseEntity
            .status(ex.getHttpStatus())
            .body(createErrorResponse(ex));
    }
}
```

### 8. **Builder Pattern**
Used extensively with Lombok and custom builders

```java
JobDTO job = JobDTO.builder()
    .title("Senior QA Engineer")
    .companyName("Google")
    .location("Mountain View")
    .build();
```

---

## 📊 Entity Relationship Model

### Core Entities

```
User (1)  ────────────┐
                      │ (1:n)
                      ▼
                   Resume
                      │
                      │
                      ▼
              Resume Embeddings
                      │
                      │ (for matching)
                      ▼
                   Job ────────┐
                      │        │
                      │        │ (1:n)
                      │        ▼
                      │    Job Embeddings
                      │
                      └────────┬─────────────┐
                               │             │
                          (1:n)│         (1:n)│
                               ▼             ▼
                         JobMatch      Application
                               │             │
                               │             │
                               └─────┬───────┘
                                     │
                                (1:n)│
                                     ▼
                          ApplicationEvents
```

---

## 🔒 Security Architecture

### Authentication Flow

```
Client Request (JWT Token)
     │
     ▼
JwtAuthenticationFilter
     │
     ├─► Extract Token
     ├─► Validate Signature
     ├─► Check Expiration
     │
     ▼
UserDetailsService
     │
     ├─► Load User from Database
     ├─► Load Authorities
     │
     ▼
SecurityContext
     │
     ├─► Set Authentication
     ├─► Proceed to Controller
     │
     ▼
@PreAuthorize / @Secured
```

### JWT Token Structure

```
Header: {
  "alg": "HS512",
  "typ": "JWT"
}

Payload: {
  "userId": 123,
  "email": "user@example.com",
  "roles": ["ROLE_USER"],
  "iat": 1686000000,
  "exp": 1686086400
}

Signature: HMACSHA512(header + payload + secret)
```

---

## 📈 Scalability Considerations

### Horizontal Scaling
- **Stateless Services**: All services are stateless
- **Session Storage**: JWT tokens (no server-side sessions)
- **Load Balancing**: Can run multiple instances behind load balancer

### Vertical Scaling
- **Database Connection Pooling**: HikariCP with configurable pool size
- **Caching Layer**: Redis for frequently accessed data
- **Async Processing**: Non-blocking operations

### Database Optimization
- **Indexing**: Strategic indexes on search/query columns
- **Query Optimization**: Lazy loading, pagination
- **Partitioning**: Future: partition jobs table by date

### API Performance
- **Response Caching**: Cache GET requests
- **Pagination**: Never return entire dataset
- **Batch Operations**: Bulk inserts for job ingestion
- **Compression**: gzip compression enabled

---

## 🔄 Data Flow for Key Scenarios

### Scenario 1: User Uploads Resume

```
1. Frontend: Upload resume PDF
                │
                ▼
2. Controller: Receive file, validate
                │
                ▼
3. Service: Parse resume (Phase 2)
                │
                ├─► Extract text
                ├─► Extract skills
                ├─► Extract experience
                │
                ▼
4. Infrastructure: Generate embeddings (Phase 3)
                │
                └─► OpenAI API
                │
                ▼
5. Repository: Save Resume + Embeddings
                │
                ▼
6. Database: INSERT INTO resumes, job_embeddings
                │
                ▼
7. Response: Return success with resume ID
```

### Scenario 2: Matching Jobs with Resume

```
1. Service: Get recent jobs (last 24 hours)
                │
                ▼
2. Repository: Query jobs from database
                │
                ▼
3. For each job:
   ├─► Get job embeddings
   ├─► Get resume embeddings
   ├─► Domain Service: Calculate similarity
   ├─► Service: Generate match analysis (AI)
   └─► Repository: Save JobMatch
                │
                ▼
4. Response: Return sorted matches
```

---

## 🧪 Testing Strategy

### Unit Testing
- **Target**: Individual methods/classes
- **Tools**: JUnit 5, Mockito
- **Isolation**: Mock all dependencies

### Integration Testing
- **Target**: Multiple layers together
- **Tools**: TestContainers, @SpringBootTest
- **Database**: Embedded PostgreSQL

### End-to-End Testing
- **Target**: Full API flow
- **Tools**: Rest-Assured, Cucumber (future)
- **Scope**: Complete request → response

---

## 📝 Code Organization Principles

### Naming Conventions

```
✅ Controllers:        *Controller        (JobController)
✅ Services:           *Service           (JobService)
✅ Repositories:       *Repository        (JobRepository)
✅ Entities:           *Entity (or plain) (Job, User)
✅ DTOs:               *DTO               (JobDTO)
✅ Mappers:            *Mapper            (JobMapper)
✅ Exceptions:         *Exception         (JobPulseException)
✅ Utilities:          *Util              (DateUtil)
```

### Package Organization

```
✅ By Feature/Domain   (Recommended)
   com.jobpulse.job
   com.jobpulse.resume
   com.jobpulse.application

❌ By Layer (Not recommended for this size)
   com.jobpulse.controller
   com.jobpulse.service
   com.jobpulse.repository
```

---

## 🔄 Transaction Management

### Declarative Transactions

```java
@Service
@Transactional  // Default: REQUIRED, PROPAGATION=REQUIRED
public class JobService {
    
    @Transactional(readOnly = true)  // Optimization for reads
    public Job findById(Long id) {
        return jobRepository.findById(id);
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void criticalOperation() {
        // Always runs in new transaction
    }
}
```

### Rollback Handling

```java
// Automatic rollback on unchecked exceptions
@Service
public class JobService {
    @Transactional
    public void process() {
        jobRepository.save(job);
        
        // Unchecked exception → automatic rollback
        throw new RuntimeException("Error");
    }
}

// Manual rollback for checked exceptions
@Service
public class JobService {
    @Transactional(rollbackFor = IOException.class)
    public void process() throws IOException {
        // IOException triggers rollback
    }
}
```

---

## 🚀 Deployment Considerations

### Containerization
- Multi-stage Docker build for optimization
- Health checks for orchestration systems
- Environment variables for configuration

### Configuration Management
- Externalized config via application-{profile}.yml
- Environment-specific properties
- Secrets management (future: HashiCorp Vault)

### Monitoring
- Application logs to file and console
- Health check endpoints
- Metrics (future: Micrometer, Prometheus)

---

## 📚 Best Practices Implemented

✅ **Single Responsibility**: Each class has one reason to change  
✅ **Dependency Injection**: Constructor-based DI via Spring  
✅ **Immutability**: Final fields, no setters where possible  
✅ **Error Handling**: Checked exceptions converted to unchecked  
✅ **Logging**: Consistent logging with SLF4J  
✅ **Documentation**: Javadoc for public APIs  
✅ **Testing**: Unit and integration tests  
✅ **Security**: Input validation, SQL injection prevention  
✅ **Performance**: Pagination, indexing, caching  
✅ **Maintainability**: Clear code structure, consistent patterns  

---

## 🎯 Future Architecture Enhancements

### Phase 2-3
- Add event-driven architecture (Spring Events)
- Implement CQRS pattern for read-heavy operations
- Add distributed tracing (Spring Cloud Sleuth)

### Phase 4-5
- Implement async message queue (RabbitMQ/Kafka)
- Add cache-aside pattern across all repositories
- Implement circuit breaker for external APIs

### Phase 6-7
- Add rate limiting (Spring Cloud Gateway)
- Implement API versioning
- Add comprehensive monitoring (Prometheus + Grafana)
- Implement database replication for HA
- Add Kubernetes deployment manifests

---

## 📞 Architecture Review Checklist

Before adding new features, ensure:

- [ ] Follows clean architecture principles
- [ ] Appropriate layer dependencies (no circular)
- [ ] DTOs separate from entities
- [ ] Transaction boundaries clear
- [ ] Exception handling proper
- [ ] Logging appropriate
- [ ] Unit tests included
- [ ] No business logic in controllers
- [ ] Repository methods named descriptively
- [ ] Documentation updated

---

**This architecture provides a solid foundation for scaling to an enterprise-grade platform.**

*Last Updated: May 23, 2026*


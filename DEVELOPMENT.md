# Development Guide

## Project Setup

### 1. Prerequisites Installation

#### macOS
```bash
# Install Homebrew if not already installed
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install Java
brew install java

# Verify Java
java -version
# Expected: Java 21+

# Install Maven
brew install maven
mvn -version
# Expected: Maven 3.9+

# Install Docker Desktop
brew install --cask docker

# Verify Docker
docker --version
docker-compose --version
```

#### Windows
- Download Java 21 from [Eclipse Temurin](https://adoptium.net/)
- Download Maven from [Maven Official](https://maven.apache.org/)
- Download Docker Desktop from [Docker](https://www.docker.com/products/docker-desktop)
- Add to PATH environment variables

#### Linux
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install -y openjdk-21-jdk maven docker.io docker-compose

# Fedora
sudo dnf install -y java-21-openjdk maven docker docker-compose
```

### 2. IDE Configuration (IntelliJ IDEA)

1. **Open Project**
   - File → Open → Navigate to project root

2. **Configure JDK**
   - File → Project Structure → Project → SDK → Add JDK 21

3. **Enable Lombok**
   - Settings → Plugins → Install Lombok
   - Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable

4. **Configure Database**
   - View → Tool Windows → Database
   - Add PostgreSQL data source

5. **Run Configuration**
   - Edit Configurations → + → Spring Boot
   - Main class: com.jobpulse.JobPulseAutoApplyApplication
   - Program arguments: `--spring.profiles.active=dev`

### 3. Database Setup

```bash
# Start PostgreSQL Container
docker-compose up -d postgres

# Wait for healthy status (30-40 seconds)
docker ps | grep postgres

# Connect to database
psql -h localhost -U postgres -d jobpulse

# View tables after application startup
\dt

# Exit
\q
```

### 4. Run Application

#### Via Maven
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

#### Via IDE
1. Right-click `JobPulseAutoApplyApplication.java`
2. Select "Run 'JobPulseAutoApplyApplication'"

#### Via Docker
```bash
docker-compose up --build jobpulse-backend
```

### 5. Verify Installation

```bash
# Health check endpoint
curl http://localhost:8080/api/health

# Swagger documentation
open http://localhost:8080/api/swagger-ui.html

# Database connection
psql -h localhost -U postgres -d jobpulse -c "SELECT version();"
```

---

## Code Standards

### Java Conventions

```java
// ✅ Good: Clear, descriptive naming
public class JobMatchingService {
    private final JobRepository jobRepository;
    
    public Optional<Job> findJobById(Long id) {
        return jobRepository.findById(id);
    }
}

// ❌ Avoid: Unclear abbreviations
public class JMS {
    private JR jr;
    public Optional<J> fJI(Long i) { ... }
}
```

### Documentation

```java
/**
 * Calculates semantic similarity between job and resume.
 *
 * Uses OpenAI embeddings to generate vectors for both job description
 * and resume content, then computes cosine similarity.
 *
 * @param job the job entity
 * @param resume the resume entity
 * @return similarity score between 0.0 and 1.0
 * @throws ExternalServiceException if embeddings service fails
 */
public double calculateSimilarity(Job job, Resume resume) {
    // Implementation
}
```

### Unit Testing

```java
@ExtendWith(MockitoExtension.class)
class JobMatchingServiceTest {
    
    @Mock
    private JobRepository jobRepository;
    
    @InjectMocks
    private JobMatchingService service;
    
    @Test
    void testCalculateMatchPercentage_WithHighSimilarity() {
        // Arrange
        Job job = createTestJob();
        Resume resume = createTestResume();
        
        // Act
        double similarity = service.calculateSimilarity(job, resume);
        
        // Assert
        assertThat(similarity).isGreaterThan(0.8);
    }
}
```

---

## API Development

### Creating a New Endpoint

#### 1. Define DTO
```java
// src/main/java/com/jobpulse/api/dto/JobSearchRequestDTO.java
@Data
@Valid
public class JobSearchRequestDTO {
    @NotBlank(message = "Search query is required")
    private String query;
    
    @Min(0)
    private int page = 0;
    
    @Max(100)
    @Min(1)
    private int pageSize = 20;
}
```

#### 2. Create Service
```java
// src/main/java/com/jobpulse/application/service/JobSearchService.java
@Service
@Transactional(readOnly = true)
public class JobSearchService {
    
    private final JobRepository jobRepository;
    
    public Page<Job> search(String query, int page, int size) {
        return jobRepository.search(query, PageRequest.of(page, size));
    }
}
```

#### 3. Create Controller
```java
// src/main/java/com/jobpulse/api/controller/JobController.java
@RestController
@RequestMapping("/jobs")
public class JobController {
    
    @GetMapping("/search")
    @Operation(summary = "Search jobs")
    public ResponseEntity<ApiResponse<PageResponse<JobDTO>>> search(
            @Valid JobSearchRequestDTO request) {
        Page<Job> jobs = jobSearchService.search(
            request.getQuery(), 
            request.getPage(), 
            request.getPageSize()
        );
        return ResponseEntity.ok(ApiResponse.success(convertToPageResponse(jobs)));
    }
}
```

---

## Database Development

### Adding a New Table

#### 1. Create Migration
```sql
-- src/main/resources/db/migration/V2__add_job_alerts_table.sql
CREATE TABLE job_alerts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    keyword VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alerts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

#### 2. Create JPA Entity
```java
// src/main/java/com/jobpulse/domain/entity/JobAlert.java
@Entity
@Table(name = "job_alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String keyword;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @CreationTimestamp
    private Instant createdAt;
}
```

#### 3. Create Repository
```java
// src/main/java/com/jobpulse/domain/repository/JobAlertRepository.java
@Repository
public interface JobAlertRepository extends JpaRepository<JobAlert, Long> {
    List<JobAlert> findByUserIdAndIsActiveTrue(Long userId);
}
```

---

## Debugging

### Enable Debug Logging
```yaml
# application-dev.yml
logging:
  level:
    com.jobpulse: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
```

### Debug Database Queries
```java
@Component
public class HibernateDebugUtil {
    public static void logQueryAndParams(Query query) {
        log.debug("SQL Query: {}", query.getQueryString());
        query.getNamedParameters().forEach((k, v) -> 
            log.debug("Parameter [{}]: {}", k, v)
        );
    }
}
```

### Remote Debugging
```bash
# Run with debug port
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
     -jar target/job-pulse-auto-apply-1.0.0.jar

# In IDE: Run → Edit Configurations → + Remote (port 5005)
```

---

## Performance Testing

### Load Testing with JMeter
```bash
# Install JMeter
brew install jmeter

# Run test plan
jmeter -n -t test_plan.jmx -l results.jtl
```

### Database Query Analysis
```sql
-- Explain query plan
EXPLAIN ANALYZE 
SELECT * FROM jobs 
WHERE company_name = 'Google' 
AND created_at > NOW() - INTERVAL '1 day';

-- Check index usage
SELECT * FROM pg_stat_user_indexes 
WHERE relname = 'jobs';
```

---

## Troubleshooting Common Issues

### Issue: "Cannot find PostgreSQL driver"
**Solution:**
```bash
mvn dependency:resolve
mvn clean install
```

### Issue: "Flyway migration fails"
**Solution:**
```sql
-- Check migration history
SELECT * FROM flyway_schema_history;

-- If corrupted, reset (DEV ONLY!)
DELETE FROM flyway_schema_history WHERE version = 1;
```

### Issue: "Connection timeout to database"
**Solution:**
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Check logs
docker logs jobpulse-postgres

# Restart
docker-compose restart postgres
```

### Issue: "Port 8080 already in use"
**Solution:**
```bash
# Find process on port 8080
lsof -i :8080

# Kill process
kill -9 <PID>

# Or change port in .env
SERVER_PORT=8081
```

---

## Useful Commands

```bash
# Build without tests
mvn clean install -DskipTests

# Run specific test
mvn test -Dtest=JobControllerTest

# Generate Javadoc
mvn javadoc:javadoc

# Check dependencies for vulnerabilities
mvn dependency-check:check

# Format code
mvn spotless:apply

# View dependency tree
mvn dependency:tree

# Update all dependencies
mvn versions:update-properties

# Deploy to Docker Hub
docker tag jobpulse-backend:1.0.0 username/jobpulse-backend:1.0.0
docker push username/jobpulse-backend:1.0.0
```

---

## Recommended Extensions (VSCode/IntelliJ)

### IntelliJ IDEA
- ✅ Lombok Plugin
- ✅ Protocol Buffers
- ✅ Docker
- ✅ Kubernetes
- ✅ Rainbow Brackets
- ✅ SonarLint

### VSCode
- ✅ Extension Pack for Java
- ✅ Spring Boot Extension Pack
- ✅ Docker
- ✅ Database Client
- ✅ Prettier
- ✅ ESLint

---

## Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [PostgreSQL Manual](https://www.postgresql.org/docs/)
- [Maven Getting Started](https://maven.apache.org/guides/getting-started/)
- [Docker Documentation](https://docs.docker.com/)
- [Flyway Documentation](https://flywaydb.org/documentation/)

---

## Getting Help

1. Check the README.md for common issues
2. Review existing code patterns
3. Check test files for usage examples
4. Refer to inline code comments
5. Check Phase documentation

Good luck! 🚀


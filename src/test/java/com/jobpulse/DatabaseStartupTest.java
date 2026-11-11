package com.jobpulse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@SpringBootTest
class DatabaseStartupTest {
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    static {
        postgres.start();
    }
    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.security.jwt.secret", () -> "test-only-signing-key-that-is-long-enough-for-hs512-keep-out-of-production-123456789");
        registry.add("jobpulse.ingestion.worker-enabled", () -> "false");
        registry.add("jobpulse.tailoring.worker-enabled", () -> "false");
        registry.add("jobpulse.feeds.include-mock", () -> "false");
        registry.add("jobpulse.feeds.greenhouse-boards", () -> "");
        registry.add("jobpulse.feeds.lever-sites", () -> "");
    }
    @org.springframework.beans.factory.annotation.Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @org.springframework.beans.factory.annotation.Autowired com.fasterxml.jackson.databind.ObjectMapper mapper;

    @org.springframework.beans.factory.annotation.Autowired com.jobpulse.domain.repository.UserRepository users;
    @org.springframework.beans.factory.annotation.Autowired org.springframework.jdbc.core.JdbcTemplate database;
    @org.springframework.beans.factory.annotation.Autowired com.jobpulse.application.service.IngestionRunService ingestionRuns;

    @Test void durableDiscoveryCoalescesRequestsAndRecoversExpiredWorkers() throws Exception {
        database.update("DELETE FROM ingestion_runs");
        try {
            String token=registerAndLogin("discovery-test");
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/jobs/ingestion"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/jobs/ingest"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/jobs/ingestion").header("Authorization","Bearer "+token))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent());
            var response=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/jobs/ingest").header("Authorization","Bearer "+token))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isAccepted()).andReturn();
            long id=mapper.readTree(response.getResponse().getContentAsString()).path("id").asLong();
            String anotherUser=registerAndLogin("discovery-other-user");
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/jobs/ingest").header("Authorization","Bearer "+anotherUser))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isAccepted())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.id").value(id))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.userId").doesNotExist())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.resume").doesNotExist());

            try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
                var one=pool.submit(()->ingestionRuns.request());
                var two=pool.submit(()->ingestionRuns.request());
                org.junit.jupiter.api.Assertions.assertEquals(id,one.get(10,java.util.concurrent.TimeUnit.SECONDS).id());
                org.junit.jupiter.api.Assertions.assertEquals(id,two.get(10,java.util.concurrent.TimeUnit.SECONDS).id());
            }
            org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("SELECT count(*) FROM ingestion_runs",Integer.class));
            org.junit.jupiter.api.Assertions.assertEquals(id,ingestionRuns.claim().orElseThrow());
            org.junit.jupiter.api.Assertions.assertTrue(ingestionRuns.claim().isEmpty());
            database.update("UPDATE ingestion_runs SET heartbeat_at=clock_timestamp()-interval '3 minutes' WHERE id=?",id);
            ingestionRuns.heartbeat(id);
            org.junit.jupiter.api.Assertions.assertTrue(ingestionRuns.claim().isEmpty());
            org.junit.jupiter.api.Assertions.assertEquals("RUNNING",ingestionRuns.get(id).status());
            database.update("UPDATE ingestion_runs SET heartbeat_at=clock_timestamp()-interval '3 minutes' WHERE id=?",id);
            org.junit.jupiter.api.Assertions.assertTrue(ingestionRuns.claim().isEmpty());
            org.junit.jupiter.api.Assertions.assertEquals("FAILED",ingestionRuns.get(id).status());
            var report=new com.jobpulse.application.service.JobIngestionService.IngestionReport(2,2,0,0,java.util.Set.of("GREENHOUSE:fixture"),java.util.Map.of(),java.time.LocalDateTime.now());
            ingestionRuns.complete(id,report,null);
            org.junit.jupiter.api.Assertions.assertEquals("FAILED",ingestionRuns.get(id).status(),"Late workers must not replace a recovered failure");
            org.junit.jupiter.api.Assertions.assertEquals(id,ingestionRuns.request().id(),"Failure cooldown must survive requests");
            database.update("UPDATE ingestion_runs SET finished_at=clock_timestamp()-interval '2 minutes' WHERE id=?",id);
            var queued=ingestionRuns.request();
            org.junit.jupiter.api.Assertions.assertNotEquals(id,queued.id());
            var restarted=new com.jobpulse.application.service.IngestionRunService(database,mapper);
            org.junit.jupiter.api.Assertions.assertEquals("QUEUED",restarted.latest().orElseThrow().status());
            org.junit.jupiter.api.Assertions.assertEquals(queued.id(),ingestionRuns.claim().orElseThrow());
            ingestionRuns.complete(queued.id(),report,null);
            org.junit.jupiter.api.Assertions.assertEquals("SUCCEEDED",restarted.latest().orElseThrow().status());
            org.junit.jupiter.api.Assertions.assertEquals(2,restarted.latest().orElseThrow().report().path("saved").asInt());
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/jobs/ingest").header("Authorization","Bearer "+token))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.id").value(queued.id()));
        } finally { database.update("DELETE FROM ingestion_runs"); }
    }

    @Test void discoveryReportsPartialAndTotalFeedFailureTruthfully() {
        database.update("DELETE FROM ingestion_runs");
        try {
            var run=ingestionRuns.request();ingestionRuns.claim();
            ingestionRuns.complete(run.id(),new com.jobpulse.application.service.JobIngestionService.IngestionReport(1,1,0,0,
                java.util.Set.of("GREENHOUSE:fixture"),java.util.Map.of("LEVER:fixture","Provider unavailable"),java.time.LocalDateTime.now()),null);
            org.junit.jupiter.api.Assertions.assertEquals("PARTIAL",ingestionRuns.get(run.id()).status());
            database.update("UPDATE ingestion_runs SET finished_at=clock_timestamp()-interval '2 minutes'");
            var retry=ingestionRuns.request();ingestionRuns.claim();
            ingestionRuns.complete(retry.id(),new com.jobpulse.application.service.JobIngestionService.IngestionReport(0,0,0,0,
                java.util.Set.of(),java.util.Map.of("LEVER:fixture","Provider unavailable"),java.time.LocalDateTime.now()),null);
            org.junit.jupiter.api.Assertions.assertEquals("FAILED",ingestionRuns.get(retry.id()).status());
            org.junit.jupiter.api.Assertions.assertTrue(ingestionRuns.get(retry.id()).error().contains("No employer feed completed"));
        } finally { database.update("DELETE FROM ingestion_runs"); }
    }

    @Test void matchingWithholdsExplicitlyDeniedSkillsWithoutMutatingTheOwnedResume() throws Exception {
        String token=registerAndLogin("skill-review-test");
        String original="Java engineer. No experience with Java. Built Selenium tests.";
        var file=new org.springframework.mock.web.MockMultipartFile("file","review.txt","text/plain",original.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var uploaded=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/resumes/upload").file(file).header("Authorization","Bearer "+token))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andReturn();
        long resumeId=mapper.readTree(uploaded.getResponse().getContentAsString()).path("id").asLong();
        var posted=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/jobs/manual").header("Authorization","Bearer "+token)
            .contentType("application/json").content(mapper.writeValueAsString(java.util.Map.of("title","Review fixture engineer","company","Explicit test fixture","description","Engineer using Java and Selenium. This is an unverified private test description."))))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andReturn();
        long jobId=mapper.readTree(posted.getResponse().getContentAsString()).path("id").asLong();
        var matches=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/matches").header("Authorization","Bearer "+token))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andReturn();
        var row=java.util.stream.StreamSupport.stream(mapper.readTree(matches.getResponse().getContentAsString()).spliterator(),false)
            .filter(value->value.path("jobId").asLong()==jobId).findFirst().orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("selenium"),mapper.convertValue(row.path("matchedSkills"),java.util.List.class));
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("java"),mapper.convertValue(row.path("skillsRequiringContextReview"),java.util.List.class));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/resumes/"+resumeId+"/original").header("Authorization","Bearer "+token))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().bytes(file.getBytes()));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/resumes/"+resumeId+"/text").header("Authorization","Bearer "+token))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.text").value(original));
    }

    @Test void migrationsAndHibernateValidationStart() {}

    @Test void logoutRevokesSignedTokensAndDisabledUsersCannotLogin() throws Exception {
        String token=registerAndLogin("revocation-test");
        String corrupt=token.substring(0,token.lastIndexOf('.')+1)+"A".repeat(86);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/profile").header("Authorization","Bearer "+corrupt))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/logout").header("Authorization","Bearer "+token))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/profile").header("Authorization","Bearer "+token))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
        var user=users.findByUsername("revocation-test").orElseThrow();user.setEnabled(false);users.saveAndFlush(user);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/login").contentType("application/json").content("{\"username\":\"revocation-test\",\"password\":\"test-password-123\"}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
    }


    @Test void upgradesHistoricalSchemaWithoutChangingResumeText() throws Exception {
        String schema="historical_upgrade_test";
        var initial=org.flywaydb.core.Flyway.configure().dataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword())
            .schemas(schema).defaultSchema(schema).target("3").load();
        initial.migrate();
        try(var connection=java.sql.DriverManager.getConnection(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword())) {
            connection.setSchema(schema);
            try(var statement=connection.createStatement()) {
                statement.executeUpdate("INSERT INTO users(id,username,email,password_hash) VALUES (42,'historical','historical@example.com','test-only')");
                statement.executeUpdate("INSERT INTO resumes(user_id,file_name,file_path,content_text,is_primary) VALUES (42,'original.txt','historical/path','Historical original resume content',true)");
            }
            var latest=org.flywaydb.core.Flyway.configure().dataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword())
                .schemas(schema).defaultSchema(schema).load();
            latest.migrate();latest.validate();
            try(var statement=connection.createStatement();var rows=statement.executeQuery("SELECT content_text,original_file FROM resumes WHERE user_id=42")) {
                org.junit.jupiter.api.Assertions.assertTrue(rows.next());
                org.junit.jupiter.api.Assertions.assertEquals("Historical original resume content",rows.getString(1));
                org.junit.jupiter.api.Assertions.assertNull(rows.getBytes(2));
            }
        }
    }

    private String registerAndLogin(String name) throws Exception {
        String body=mapper.writeValueAsString(java.util.Map.of("username",name,"email",name+"@example.com","password","test-password-123"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/register").contentType("application/json").content(body))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        var response=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/login").contentType("application/json").content(body))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andReturn();
        return mapper.readTree(response.getResponse().getContentAsString()).path("token").asText();
    }

    @Test void authenticatedWorkflowsAreIsolated() throws Exception {
        String alice=registerAndLogin("alice"), bob=registerAndLogin("bob");
        var upload=new org.springframework.mock.web.MockMultipartFile("file","cv.txt","text/plain","Senior Java engineer with Spring Boot".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var response=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/resumes/upload").file(upload).header("Authorization","Bearer "+alice))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andReturn();
        long id=mapper.readTree(response.getResponse().getContentAsString()).path("id").asLong();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/resumes/"+id+"/text").header("Authorization","Bearer "+bob))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/resumes/"+id+"/original").header("Authorization","Bearer "+alice))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().bytes(upload.getBytes()));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/resumes/"+id+"/original").header("Authorization","Bearer "+bob))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/resumes"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/resumes").header("Authorization","Bearer broken.signature.token"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
        var manual=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/jobs/manual").header("Authorization","Bearer "+alice)
            .contentType("application/json").content(mapper.writeValueAsString(java.util.Map.of("title","Private opportunity","company","Example","description","Java Spring Boot backend engineer with testing experience"))))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andReturn();
        long jobId=mapper.readTree(manual.getResponse().getContentAsString()).path("id").asLong();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/jobs/"+jobId).header("Authorization","Bearer "+alice))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.description").value("Java Spring Boot backend engineer with testing experience"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.postingStatus").value("MANUAL_UNVERIFIED"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/jobs/"+jobId).header("Authorization","Bearer "+bob))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/jobs/"+jobId))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());

        database.update("INSERT INTO jobs(source_id,source_job_id,title,company_name,job_description) SELECT id,'legacy-no-owner','Unowned legacy job','Historical','Not attributable to any user' FROM job_sources WHERE name='MANUAL'");
        String savedBody=mapper.writeValueAsString(java.util.Map.of("resumeId",id,"jobId",jobId,"content","Java engineer","approved",true));
        var saved=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/tailoring/documents").header("Authorization","Bearer "+alice).contentType("application/json").content(savedBody))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andReturn();
        long documentId=mapper.readTree(saved.getResponse().getContentAsString()).path("id").asLong();
        var word = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/tailoring/documents/"+documentId+"/download?format=docx").header("Authorization","Bearer "+alice))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")).andReturn();
        try (var parsed = new org.apache.poi.xwpf.usermodel.XWPFDocument(new java.io.ByteArrayInputStream(word.getResponse().getContentAsByteArray()));
             var extracted = new org.apache.poi.xwpf.extractor.XWPFWordExtractor(parsed)) {
            org.junit.jupiter.api.Assertions.assertEquals("Java engineer", extracted.getText().trim());
        }
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/tailoring/documents/"+documentId+"/download?format=docx").header("Authorization","Bearer "+bob))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/tailoring/documents/"+documentId+"/download?format=pdf").header("Authorization","Bearer "+alice))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/tailoring/documents/"+documentId+"/download").header("Authorization","Bearer "+bob))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/resumes/"+id+"/text").header("Authorization","Bearer "+alice))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.text").value("Senior Java engineer with Spring Boot"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/resumes/"+id+"/primary").header("Authorization","Bearer "+bob))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        String completion=mapper.writeValueAsString(java.util.Map.of("jobId",jobId,"completed",true));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/applications/manual").header("Authorization","Bearer "+alice).contentType("application/json").content(completion))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.applicationStatus").value("USER_REPORTED_SUBMITTED"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.providerConfirmed").value(false));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/applications/manual").header("Authorization","Bearer "+alice).contentType("application/json").content(completion))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isConflict());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/applications").header("Authorization","Bearer "+bob))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().json("[]"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/profile").header("Authorization","Bearer "+alice).contentType("application/json").content("{\"fullName\":\"Alice\",\"targetRoles\":\"SDET\",\"targetLocations\":\"Toronto\"}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.targetRoles").value("SDET"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/jobs").header("Authorization","Bearer "+bob))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().json("[]"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/users").header("Authorization","Bearer "+alice))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
    }

    @org.springframework.beans.factory.annotation.Autowired com.jobpulse.application.service.TailoringTaskService taskQueue;
    @org.springframework.beans.factory.annotation.Autowired com.jobpulse.domain.repository.ResumeRepository resumeRepository;
    @org.springframework.beans.factory.annotation.Autowired com.jobpulse.domain.repository.JobRepository jobRepository;

    private record QueueFixture(String token,long uid,long resumeId,long jobId) {}
    private QueueFixture queueFixture(String name) throws Exception {
        String token=registerAndLogin(name);
        var upload=new org.springframework.mock.web.MockMultipartFile("file","original.txt","text/plain","Senior Java engineer with Spring Boot".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var response=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/resumes/upload").file(upload).header("Authorization","Bearer "+token))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andReturn();
        long resume=mapper.readTree(response.getResponse().getContentAsString()).path("id").asLong();
        var manual=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/jobs/manual").header("Authorization","Bearer "+token).contentType("application/json")
            .content("{\"title\":\"Queue fixture\",\"company\":\"Test fixture\",\"description\":\"Java Spring Boot testing experience for backend services\"}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andReturn();
        long job=mapper.readTree(manual.getResponse().getContentAsString()).path("id").asLong();
        return new QueueFixture(token,users.findByUsername(name).orElseThrow().getId(),resume,job);
    }

    @Test void queueIsolationCancellationAndSnapshotSaving() throws Exception {
        var alice=queueFixture("queue-alice"); var bob=queueFixture("queue-bob");
        try {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/tailoring/tasks"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
            var response=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/tailoring/tasks/"+alice.jobId()).header("Authorization","Bearer "+alice.token()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isAccepted())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status").value("QUEUED")).andReturn();
            long id=mapper.readTree(response.getResponse().getContentAsString()).path("id").asLong();
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/tailoring/"+alice.jobId()).header("Authorization","Bearer "+alice.token()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isConflict());
            for(String route: new String[]{"/tailoring/tasks/"+id,"/tailoring/tasks/"+id+"/cancel"}) {
                var request=route.endsWith("cancel")?org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(route):org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(route);
                mvc.perform(request.header("Authorization","Bearer "+bob.token())).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
            }
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/tailoring/tasks/"+alice.jobId()).header("Authorization","Bearer "+bob.token()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/tailoring/tasks").header("Authorization","Bearer "+bob.token()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().json("[]"));
            database.update("UPDATE resumes SET content_text='Changed after enqueue' WHERE id=?",alice.resumeId());
            database.update("UPDATE jobs SET job_description='Changed job after enqueue' WHERE id=?",alice.jobId());
            var work=taskQueue.claim().orElseThrow();
            org.junit.jupiter.api.Assertions.assertEquals("Senior Java engineer with Spring Boot",work.original());
            org.junit.jupiter.api.Assertions.assertEquals("Java Spring Boot testing experience for backend services",work.description());
            var second=taskQueue.enqueue(bob.uid(),bob.jobId());
            org.junit.jupiter.api.Assertions.assertTrue(taskQueue.claim().isEmpty(),"A second model call cannot run concurrently");
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/tailoring/tasks/"+id+"/cancel").header("Authorization","Bearer "+alice.token()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status").value("CANCEL_REQUESTED"));
            org.junit.jupiter.api.Assertions.assertTrue(taskQueue.claim().isEmpty(),"Cancellation retains the running slot until completion");
            taskQueue.complete(id,"must be discarded",null);
            org.junit.jupiter.api.Assertions.assertEquals("CANCELLED",taskQueue.get(alice.uid(),id).status());
            org.junit.jupiter.api.Assertions.assertNull(taskQueue.get(alice.uid(),id).draft());
            var bobWork=taskQueue.claim().orElseThrow();
            database.update("UPDATE resumes SET content_text='Bob changed his editable resume' WHERE id=?",bob.resumeId());
            taskQueue.complete(bobWork.id(),bobWork.original(),null);
            database.update("UPDATE jobs SET is_archived=true WHERE id=?",bob.jobId());
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/tailoring/tasks/"+bob.jobId()).header("Authorization","Bearer "+bob.token()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
            String body=mapper.writeValueAsString(java.util.Map.of("resumeId",bob.resumeId(),"jobId",bob.jobId(),"content",bobWork.original(),"approved",false,"taskId",second.id()));
            var saved=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/tailoring/documents").header("Authorization","Bearer "+bob.token()).contentType("application/json").content(body))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.originalSnapshot").value(bobWork.original())).andReturn();
            long savedId=mapper.readTree(saved.getResponse().getContentAsString()).path("id").asLong();
            String nextVersion=mapper.writeValueAsString(java.util.Map.of("resumeId",bob.resumeId(),"jobId",bob.jobId(),"content",bobWork.original(),"sourceDocumentId",savedId));
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/tailoring/documents").header("Authorization","Bearer "+bob.token()).contentType("application/json").content(nextVersion))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.originalSnapshot").value(bobWork.original()));
            org.junit.jupiter.api.Assertions.assertThrows(org.springframework.web.server.ResponseStatusException.class,
                ()->taskQueue.originalForSave(alice.uid(),second.id(),alice.resumeId(),alice.jobId()));
            for(String reference:new String[]{"taskId","sourceDocumentId"}) {
                String foreignSource=mapper.writeValueAsString(java.util.Map.of("resumeId",alice.resumeId(),"jobId",alice.jobId(),"content","Own edited text",reference,reference.equals("taskId")?second.id():savedId));
                mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/tailoring/documents").header("Authorization","Bearer "+alice.token()).contentType("application/json").content(foreignSource))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
            }
            var queued=taskQueue.enqueue(alice.uid(),alice.jobId());
            org.junit.jupiter.api.Assertions.assertEquals("CANCELLED",taskQueue.cancel(alice.uid(),queued.id()).status());
            org.junit.jupiter.api.Assertions.assertTrue(taskQueue.claim().isEmpty());
        } finally { database.update("DELETE FROM tailoring_tasks"); }
    }

    @Test void interruptedTasksRecoverWithoutAcceptingLateResults() throws Exception {
        var fixture=queueFixture("queue-recovery");
        try {
            var first=taskQueue.enqueue(fixture.uid(),fixture.jobId()); taskQueue.claim().orElseThrow();
            database.update("UPDATE tailoring_tasks SET started_at=clock_timestamp()-interval '6 minutes' WHERE id=?",first.id());
            // A fresh service represents a restarted process; all state is in PostgreSQL.
            var restarted=new com.jobpulse.application.service.TailoringTaskService(database,resumeRepository,jobRepository);
            org.junit.jupiter.api.Assertions.assertTrue(restarted.claim().isEmpty());
            org.junit.jupiter.api.Assertions.assertEquals("FAILED",restarted.get(fixture.uid(),first.id()).status());
            restarted.complete(first.id(),"late result",null);
            org.junit.jupiter.api.Assertions.assertNull(restarted.get(fixture.uid(),first.id()).draft());
            var retry=restarted.enqueue(fixture.uid(),fixture.jobId()); restarted.claim().orElseThrow();
            restarted.cancel(fixture.uid(),retry.id());
            database.update("UPDATE tailoring_tasks SET started_at=clock_timestamp()-interval '6 minutes' WHERE id=?",retry.id());
            restarted.claim();
            org.junit.jupiter.api.Assertions.assertEquals("CANCELLED",restarted.get(fixture.uid(),retry.id()).status());
        } finally { database.update("DELETE FROM tailoring_tasks"); }
    }

    @Test void workerFailureIsSafeAndExplicitRetryCanSucceed() throws Exception {
        var fixture=queueFixture("queue-worker");
        var model=org.mockito.Mockito.mock(com.jobpulse.application.service.ResumeTailoringService.class);
        var worker=new com.jobpulse.application.service.TailoringTaskWorker(taskQueue,model);
        try {
            var first=taskQueue.enqueue(fixture.uid(),fixture.jobId());
            org.mockito.Mockito.when(model.tailor(org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new IllegalStateException("sensitive provider response"));
            worker.processNext();
            var failed=taskQueue.get(fixture.uid(),first.id());
            org.junit.jupiter.api.Assertions.assertEquals("FAILED",failed.status());
            org.junit.jupiter.api.Assertions.assertFalse(failed.error().contains("sensitive"));
            var retry=taskQueue.enqueue(fixture.uid(),fixture.jobId());
            org.mockito.Mockito.doReturn("Senior Java engineer with Spring Boot").when(model).tailor(org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString());
            worker.processNext();
            org.junit.jupiter.api.Assertions.assertEquals("SUCCEEDED",taskQueue.get(fixture.uid(),retry.id()).status());
            org.junit.jupiter.api.Assertions.assertEquals("Senior Java engineer with Spring Boot",taskQueue.get(fixture.uid(),retry.id()).draft());
            var oversized=taskQueue.enqueue(fixture.uid(),fixture.jobId());
            org.mockito.Mockito.doReturn("a".repeat(100001)).when(model).tailor(org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString());
            worker.processNext();
            org.junit.jupiter.api.Assertions.assertEquals("FAILED",taskQueue.get(fixture.uid(),oversized.id()).status());
            org.junit.jupiter.api.Assertions.assertNull(taskQueue.get(fixture.uid(),oversized.id()).draft());
            org.junit.jupiter.api.Assertions.assertTrue(taskQueue.get(fixture.uid(),oversized.id()).error().contains("editor limit"));
        } finally { database.update("DELETE FROM tailoring_tasks"); }
    }

    @Test void concurrentEnqueueHasOnlyOneWinner() throws Exception {
        var fixture=queueFixture("queue-concurrent");
        try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var start=new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<Integer> attempt=()-> { start.await(); try { taskQueue.enqueue(fixture.uid(),fixture.jobId()); return 202; }
                catch(org.springframework.web.server.ResponseStatusException e) { return e.getStatusCode().value(); } };
            var first=executor.submit(attempt); var second=executor.submit(attempt); start.countDown();
            org.junit.jupiter.api.Assertions.assertEquals(java.util.Set.of(202,409),java.util.Set.of(first.get(10,java.util.concurrent.TimeUnit.SECONDS),second.get(10,java.util.concurrent.TimeUnit.SECONDS)));
            org.junit.jupiter.api.Assertions.assertEquals(1,taskQueue.list(fixture.uid()).size());
        } finally { database.update("DELETE FROM tailoring_tasks"); }
    }

    @Test void fullQueueRejectsAdditionalWorkWithoutDroppingExistingTasks() throws Exception {
        var fixture=queueFixture("queue-capacity");
        try {
            for(int i=0;i<20;i++) {
                String name="queue-seed-"+i;
                long uid=database.queryForObject("INSERT INTO users(username,email,password_hash) VALUES (?,?,?) RETURNING id",Long.class,name,name+"@example.com","test-only");
                long resume=database.queryForObject("INSERT INTO resumes(user_id,file_name,file_path,content_text,is_primary) VALUES (?,'fixture.txt','test-only','Fixture resume',true) RETURNING id",Long.class,uid);
                database.update("INSERT INTO tailoring_tasks(user_id,resume_id,job_id,original_snapshot,description_snapshot) VALUES (?,?,?,'Fixture resume','Fixture job description')",uid,resume,fixture.jobId());
            }
            var failure=org.junit.jupiter.api.Assertions.assertThrows(org.springframework.web.server.ResponseStatusException.class,()->taskQueue.enqueue(fixture.uid(),fixture.jobId()));
            org.junit.jupiter.api.Assertions.assertEquals(429,failure.getStatusCode().value());
            org.junit.jupiter.api.Assertions.assertEquals(20L,database.queryForObject("SELECT count(*) FROM tailoring_tasks",Long.class));
        } finally { database.update("DELETE FROM tailoring_tasks"); }
    }

    @org.springframework.beans.factory.annotation.Autowired com.jobpulse.infrastructure.adapter.FeedRetryStateStore persistentRetry;

    @Test void feedRetryStateSurvivesRecreationAndSerializesConcurrentFailures() throws Exception {
        String source="TEST:persistent-retry";
        var now=java.time.Instant.parse("2026-11-10T12:00:00.123456789Z");
        try {
            persistentRetry.recordFailure(source,now,null);
            var recreated=new com.jobpulse.infrastructure.adapter.JdbcFeedRetryStateStore(database);
            var first=recreated.find(source).orElseThrow();
            org.junit.jupiter.api.Assertions.assertEquals(1,first.attempts());
            org.junit.jupiter.api.Assertions.assertEquals(now.plusSeconds(60),first.nextAttempt());
            persistentRetry.recordFailure(source,now.plusSeconds(60),java.time.Instant.MAX);
            org.junit.jupiter.api.Assertions.assertEquals(java.time.Instant.MAX,recreated.find(source).orElseThrow().nextAttempt());
            try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)) {
                var start=new java.util.concurrent.CountDownLatch(1);
                java.util.concurrent.Callable<Void> failure=()-> { start.await(); persistentRetry.recordFailure(source,now.plusSeconds(120),null);return null; };
                var a=executor.submit(failure);var b=executor.submit(failure);start.countDown();
                a.get(10,java.util.concurrent.TimeUnit.SECONDS);b.get(10,java.util.concurrent.TimeUnit.SECONDS);
            }
            var after=recreated.find(source).orElseThrow();
            org.junit.jupiter.api.Assertions.assertEquals(4,after.attempts());
            org.junit.jupiter.api.Assertions.assertEquals(java.time.Instant.MAX,after.nextAttempt());
            persistentRetry.clear(source);
            org.junit.jupiter.api.Assertions.assertTrue(recreated.find(source).isEmpty());
            persistentRetry.recordFailure(source,now,null);
            org.junit.jupiter.api.Assertions.assertEquals(1,recreated.find(source).orElseThrow().attempts());
        } finally { persistentRetry.clear(source); }
    }
}

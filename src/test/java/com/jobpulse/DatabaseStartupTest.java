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
        registry.add("jobpulse.feeds.include-mock", () -> "false");
        registry.add("jobpulse.feeds.greenhouse-boards", () -> "");
        registry.add("jobpulse.feeds.lever-sites", () -> "");
    }
    @org.springframework.beans.factory.annotation.Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @org.springframework.beans.factory.annotation.Autowired com.fasterxml.jackson.databind.ObjectMapper mapper;

    @org.springframework.beans.factory.annotation.Autowired com.jobpulse.domain.repository.UserRepository users;
    @org.springframework.beans.factory.annotation.Autowired org.springframework.jdbc.core.JdbcTemplate database;
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
        database.update("INSERT INTO jobs(source_id,source_job_id,title,company_name,job_description) SELECT id,'legacy-no-owner','Unowned legacy job','Historical','Not attributable to any user' FROM job_sources WHERE name='MANUAL'");
        String savedBody=mapper.writeValueAsString(java.util.Map.of("resumeId",id,"jobId",jobId,"content","Java engineer","approved",true));
        var saved=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/tailoring/documents").header("Authorization","Bearer "+alice).contentType("application/json").content(savedBody))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andReturn();
        long documentId=mapper.readTree(saved.getResponse().getContentAsString()).path("id").asLong();
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
}

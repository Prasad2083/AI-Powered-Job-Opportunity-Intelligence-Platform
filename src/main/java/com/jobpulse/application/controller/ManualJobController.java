package com.jobpulse.application.controller;

import com.jobpulse.domain.entity.Job;
import com.jobpulse.domain.entity.JobSource;
import com.jobpulse.domain.repository.JobRepository;
import com.jobpulse.domain.repository.JobSourceRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Manual job import is a personal drafting aid, not proof that a job posting is live.
 */
@RestController
@RequestMapping("/jobs/manual")
public class ManualJobController {
    private final JobRepository jobs;
    private final JobSourceRepository sources;
    public ManualJobController(JobRepository jobs, JobSourceRepository sources) {
        this.jobs=jobs; this.sources=sources;
    }
    public record ManualJobRequest(
        @NotBlank @Size(max=255) String title,
        @NotBlank @Size(max=255) String company,
        @NotBlank @Size(min=40,max=40000) String description,
        @Size(max=255) String location,
        @Size(max=2048) String applyUrl
    ) {}
    @PostMapping
    @Transactional
    public ResponseEntity<Map<String,Object>> create(@Valid @RequestBody ManualJobRequest input) {
        String url=input.applyUrl()==null?"":input.applyUrl().trim();
        if (!url.isBlank() && !url.matches("https://[^\\s]+"))
            return ResponseEntity.badRequest().body(Map.of("error","Apply URL must use HTTPS"));
        JobSource source=sources.findByName("MANUAL")
            .orElseGet(()->sources.save(JobSource.builder().name("MANUAL").isEnabled(true).build()));
        Job job=Job.builder().source(source).sourceJobId("manual-"+UUID.randomUUID())
            .title(input.title().trim()).companyName(input.company().trim())
            .jobDescription(input.description().trim()).location(input.location())
            .applyUrl(url.isBlank()?null:url).postedDate(LocalDateTime.now())
            .applied(false).isArchived(false).build();
        Job saved=jobs.save(job);
        return ResponseEntity.ok(Map.of("id",saved.getId(),"message","Saved for matching; posting not verified"));
    }
}

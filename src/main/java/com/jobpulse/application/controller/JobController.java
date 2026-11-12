package com.jobpulse.application.controller;

import com.jobpulse.application.service.AutoApplyService;
import com.jobpulse.application.service.IngestionRunService;
import com.jobpulse.domain.entity.Job;
import com.jobpulse.domain.repository.JobRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobRepository jobRepository;
    private final com.jobpulse.domain.repository.UserRepository users;
    private final IngestionRunService ingestionRuns;
    private final AutoApplyService autoApplyService;

    public JobController(JobRepository jobRepository,
                         IngestionRunService ingestionRuns,
                         AutoApplyService autoApplyService, com.jobpulse.domain.repository.UserRepository users) {
        this.jobRepository = jobRepository;
        this.ingestionRuns = ingestionRuns;
        this.autoApplyService = autoApplyService;
        this.users = users;
    }

    @GetMapping
    public ResponseEntity<List<java.util.Map<String,Object>>> listJobs(org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(jobRepository.findVisible(users.findByUsername(auth.getName()).orElseThrow().getId()).stream().map(job -> {
            java.util.Map<String,Object> item = new java.util.LinkedHashMap<>();
            item.put("lastSeenAt",job.getLastSeenAt()); item.put("postedDate",job.getPostedDate()); item.put("stale",stale(job));
            item.put("id", job.getId()); item.put("title",job.getTitle()); item.put("companyName",job.getCompanyName()); item.put("location",job.getLocation()); item.put("source", job.getSource().getName()); item.put("postingStatus", postingStatus(job)); item.put("applyUrl",job.getApplyUrl());
            return item;
        }).toList());
    }

    public record JobDetails(Long id, String title, String companyName, String location,
        String description, String source, String postingStatus, String applyUrl,
        java.time.LocalDateTime postedDate, java.time.LocalDateTime lastSeenAt, boolean stale) {}

    @GetMapping("/{id}")
    public ResponseEntity<JobDetails> details(@PathVariable Long id, org.springframework.security.core.Authentication auth) {
        Long uid = users.findByUsername(auth.getName()).orElseThrow().getId();
        return ResponseEntity.of(jobRepository.findVisibleById(id, uid).map(job -> new JobDetails(
            job.getId(), job.getTitle(), job.getCompanyName(), job.getLocation(), job.getJobDescription(),
            job.getSource().getName(), postingStatus(job), job.getApplyUrl(), job.getPostedDate(),
            job.getLastSeenAt(), stale(job))));
    }

    private static boolean stale(Job job) {
        return job.getLastSeenAt() == null || job.getLastSeenAt().isBefore(java.time.LocalDateTime.now().minusDays(7));
    }

    private static String postingStatus(Job job) {
        String source = job.getSource().getName();
        if (source.equals("MANUAL")) return "MANUAL_UNVERIFIED";
        if (source.equals("MOCK")) return "MOCK_NOT_A_VACANCY";
        return job.getLastSeenAt() != null && (source.startsWith("GREENHOUSE:") || source.startsWith("LEVER:"))
            ? "PUBLIC_FEED_RECHECK_BEFORE_APPLYING" : "SOURCE_UNVERIFIED";
    }

    @PostMapping("/ingest")
    public ResponseEntity<IngestionRunService.Run> ingestNow() {
        var run=ingestionRuns.request();
        return (run.status().equals("QUEUED") || run.status().equals("RUNNING")
            ? ResponseEntity.accepted() : ResponseEntity.ok()).body(run);
    }

    @GetMapping("/ingestion")
    public ResponseEntity<IngestionRunService.Run> latestIngestion() {
        return ingestionRuns.latest().map(ResponseEntity::ok).orElseGet(()->ResponseEntity.noContent().build());
    }

    @GetMapping("/ingestion/{id}")
    public IngestionRunService.Run ingestion(@PathVariable long id) { return ingestionRuns.get(id); }

    @PostMapping("/autoapply")
    public ResponseEntity<String> autoApplyNow() {
        return ResponseEntity.status(501).body("Auto-apply disabled until provider integration and per-user approval");
    }
}


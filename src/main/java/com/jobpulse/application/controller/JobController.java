package com.jobpulse.application.controller;

import com.jobpulse.application.service.AutoApplyService;
import com.jobpulse.application.service.JobIngestionService;
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
    private final JobIngestionService ingestionService;
    private final AutoApplyService autoApplyService;

    public JobController(JobRepository jobRepository,
                         JobIngestionService ingestionService,
                         AutoApplyService autoApplyService, com.jobpulse.domain.repository.UserRepository users) {
        this.jobRepository = jobRepository;
        this.ingestionService = ingestionService;
        this.autoApplyService = autoApplyService;
        this.users = users;
    }

    @GetMapping
    public ResponseEntity<List<java.util.Map<String,Object>>> listJobs(org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(jobRepository.findVisible(users.findByUsername(auth.getName()).orElseThrow().getId()).stream().map(job -> {
            java.util.Map<String,Object> item = new java.util.LinkedHashMap<>();
            item.put("lastSeenAt",job.getLastSeenAt()); item.put("postedDate",job.getPostedDate()); item.put("stale",job.getLastSeenAt()==null || job.getLastSeenAt().isBefore(java.time.LocalDateTime.now().minusDays(7)));
            item.put("id", job.getId()); item.put("title",job.getTitle()); item.put("companyName",job.getCompanyName()); item.put("location",job.getLocation()); item.put("source", job.getSource().getName()); item.put("postingStatus", job.getSource().getName().equals("MANUAL") ? "MANUAL_UNVERIFIED" : job.getSource().getName().equals("MOCK") ? "MOCK_NOT_A_VACANCY" : "PUBLIC_FEED_RECHECK_BEFORE_APPLYING"); item.put("applyUrl",job.getApplyUrl());
            return item;
        }).toList());
    }

    @PostMapping("/ingest")
    public ResponseEntity<JobIngestionService.IngestionReport> ingestNow() {
        return ResponseEntity.ok(ingestionService.ingestJobs());
    }

    @PostMapping("/autoapply")
    public ResponseEntity<String> autoApplyNow() {
        return ResponseEntity.status(501).body("Auto-apply disabled until provider integration and per-user approval");
    }
}


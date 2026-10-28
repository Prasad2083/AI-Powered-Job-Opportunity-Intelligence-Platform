package com.jobpulse.application.controller;

import com.jobpulse.application.service.AutoApplyService;
import com.jobpulse.application.service.JobIngestionService;
import com.jobpulse.domain.entity.Job;
import com.jobpulse.domain.repository.JobRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobRepository jobRepository;
    private final JobIngestionService ingestionService;
    private final AutoApplyService autoApplyService;

    public JobController(JobRepository jobRepository,
                         JobIngestionService ingestionService,
                         AutoApplyService autoApplyService) {
        this.jobRepository = jobRepository;
        this.ingestionService = ingestionService;
        this.autoApplyService = autoApplyService;
    }

    @GetMapping
    public ResponseEntity<List<Job>> listJobs() {
        return ResponseEntity.ok(jobRepository.findAll());
    }

    @PostMapping("/ingest")
    public ResponseEntity<String> ingestNow() {
        ingestionService.ingestJobs();
        return ResponseEntity.ok("Ingestion triggered");
    }

    @PostMapping("/autoapply")
    public ResponseEntity<String> autoApplyNow() {
        return ResponseEntity.status(501).body("Auto-apply disabled until provider integration and per-user approval");
    }
}


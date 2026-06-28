package com.jobpulse.application.controller;

import com.jobpulse.domain.entity.ApplicationRecord;
import com.jobpulse.domain.repository.ApplicationRecordRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Phase 7: Applications Controller for frontend
 */
@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationRecordRepository applicationRecordRepository;

    public ApplicationController(ApplicationRecordRepository applicationRecordRepository) {
        this.applicationRecordRepository = applicationRecordRepository;
    }

    @GetMapping
    public ResponseEntity<List<ApplicationRecord>> listApplications() {
        return ResponseEntity.ok(applicationRecordRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationRecord> getApplication(@PathVariable Long id) {
        return ResponseEntity.of(applicationRecordRepository.findById(id));
    }
}


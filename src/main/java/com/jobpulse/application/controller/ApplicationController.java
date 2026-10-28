package com.jobpulse.application.controller;

import com.jobpulse.domain.entity.ApplicationRecord;
import com.jobpulse.domain.repository.ApplicationRecordRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.core.Authentication;
import com.jobpulse.domain.repository.UserRepository;

/**
 * Phase 7: Applications Controller for frontend
 */
@RestController
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationRecordRepository applicationRecordRepository;
    private final UserRepository users;

    public ApplicationController(ApplicationRecordRepository applicationRecordRepository, UserRepository users) {
        this.applicationRecordRepository = applicationRecordRepository;
        this.users = users;
    }

    @GetMapping
    public ResponseEntity<List<ApplicationRecord>> listApplications(Authentication authentication) {
        return ResponseEntity.ok(applicationRecordRepository.findByUserId(users.findByUsername(authentication.getName()).orElseThrow().getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationRecord> getApplication(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.of(applicationRecordRepository.findByIdAndUserId(id, users.findByUsername(authentication.getName()).orElseThrow().getId()));
    }
}

